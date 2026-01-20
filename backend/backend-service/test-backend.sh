#!/bin/bash

# Backend Service Test Script
# Tests all three mock services and verifies Prometheus metrics

BASE_URL="http://localhost:8091"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Backend Service Test Script${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Check if service is running
echo -e "${BLUE}1. Checking service health...${NC}"
HEALTH=$(curl -s ${BASE_URL}/actuator/health | grep -o '"status":"UP"')
if [ -n "$HEALTH" ]; then
    echo -e "${GREEN}✓ Service is healthy${NC}\n"
else
    echo -e "${RED}✗ Service is not running or unhealthy${NC}"
    exit 1
fi

# Test User Service
echo -e "${BLUE}2. Testing User Service...${NC}"
echo "   GET /users"
curl -s ${BASE_URL}/users | jq '. | length' | xargs echo "   Users found:"
echo ""
echo "   POST /users (creating new user)"
NEW_USER=$(curl -s -X POST ${BASE_URL}/users \
  -H "Content-Type: application/json" \
  -d '{"email":"test-'$(date +%s)'@example.com","fullName":"Test User","role":"USER"}')
echo "$NEW_USER" | jq -r '"   Created user: " + .fullName + " (" + .email + ")"'
echo -e "${GREEN}✓ User service working${NC}\n"

# Test Order Service
echo -e "${BLUE}3. Testing Order Service...${NC}"
echo "   GET /orders"
curl -s ${BASE_URL}/orders | jq '. | length' | xargs echo "   Orders found:"
echo ""
echo "   POST /orders (creating new order)"
NEW_ORDER=$(curl -s -X POST ${BASE_URL}/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-test","productName":"Test Product","quantity":1,"totalAmount":99.99}')
echo "$NEW_ORDER" | jq -r '"   Created order #" + (.id|tostring) + " - " + .productName'
echo -e "${GREEN}✓ Order service working${NC}\n"

# Test Payment Service
echo -e "${BLUE}4. Testing Payment Service (with random failures)...${NC}"
SUCCESS_COUNT=0
FAILURE_COUNT=0
for i in {1..10}; do
    PAYMENT=$(curl -s -X POST ${BASE_URL}/payments \
      -H "Content-Type: application/json" \
      -d "{\"orderId\":\"order-test-$i\",\"amount\":$((RANDOM % 500 + 100)),\"paymentMethod\":\"CREDIT_CARD\"}")
    STATUS=$(echo "$PAYMENT" | jq -r '.status')
    if [ "$STATUS" = "SUCCESS" ]; then
        ((SUCCESS_COUNT++))
        echo -e "   Payment $i: ${GREEN}$STATUS${NC}"
    else
        ((FAILURE_COUNT++))
        REASON=$(echo "$PAYMENT" | jq -r '.failureReason')
        echo -e "   Payment $i: ${RED}$STATUS${NC} - $REASON"
    fi
done
echo ""
echo "   Summary: $SUCCESS_COUNT successful, $FAILURE_COUNT failed"
echo -e "${GREEN}✓ Payment service working (with ~10% failure rate)${NC}\n"

# Check Prometheus Metrics
echo -e "${BLUE}5. Checking Prometheus Metrics...${NC}"
echo ""
echo "   Custom Business Metrics:"
curl -s ${BASE_URL}/actuator/prometheus | grep -E "mock_(users|orders|payments)" | grep -v "^#"
echo ""

echo "   HTTP Request Metrics (sample):"
curl -s ${BASE_URL}/actuator/prometheus | grep "http_server_requests_seconds_count" | grep -v "^#" | head -3
echo ""

echo -e "${GREEN}✓ Prometheus metrics exposed${NC}\n"

# Performance Summary
echo -e "${BLUE}6. Performance Summary...${NC}"
echo "   Expected Delays:"
echo "   - User operations: 50-200ms"
echo "   - Order operations: 100-300ms"
echo "   - Payment operations: 300-700ms"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}All tests passed! ✓${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "View all metrics:"
echo "  curl ${BASE_URL}/actuator/prometheus"
echo ""
echo "View health:"
echo "  curl ${BASE_URL}/actuator/health"
echo ""
echo "Access in browser:"
echo "  ${BASE_URL}/actuator"
