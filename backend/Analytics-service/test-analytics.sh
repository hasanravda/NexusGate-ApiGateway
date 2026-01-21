#!/bin/bash

# Analytics Service Test Script
# Tests all endpoints of the Analytics Service

BASE_URL="http://localhost:8085"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================="
echo "Analytics Service Test Script"
echo "=================================="
echo ""

# Function to print test results
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASSED${NC}: $2"
    else
        echo -e "${RED}✗ FAILED${NC}: $2"
    fi
}

# Test 1: Health Check
echo "Test 1: Health Check"
response=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_URL}/actuator/health)
if [ "$response" = "200" ]; then
    print_result 0 "Health check"
else
    print_result 1 "Health check (Expected 200, got $response)"
fi
echo ""

# Test 2: Prometheus Metrics
echo "Test 2: Prometheus Metrics Endpoint"
response=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_URL}/actuator/prometheus)
if [ "$response" = "200" ]; then
    print_result 0 "Prometheus metrics endpoint"
else
    print_result 1 "Prometheus metrics endpoint (Expected 200, got $response)"
fi
echo ""

# Test 3: Log Controller Health
echo "Test 3: Log Controller Health"
response=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_URL}/logs/health)
if [ "$response" = "200" ]; then
    print_result 0 "Log controller health"
else
    print_result 1 "Log controller health (Expected 200, got $response)"
fi
echo ""

# Test 4: Send Test Log Event (Success)
echo "Test 4: Send Test Log Event - Success (Status 200)"
response=$(curl -s -o /dev/null -w "%{http_code}" -X POST ${BASE_URL}/logs \
    -H "Content-Type: application/json" \
    -d '{
        "apiKeyId": 1,
        "serviceRouteId": 2,
        "method": "GET",
        "path": "/api/users/123",
        "status": 200,
        "latencyMs": 145,
        "rateLimited": false,
        "clientIp": "192.168.1.100",
        "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'"
    }')
if [ "$response" = "202" ]; then
    print_result 0 "Send log event (success)"
else
    print_result 1 "Send log event (success) (Expected 202, got $response)"
fi
echo ""

# Test 5: Send Test Log Event (Error)
echo "Test 5: Send Test Log Event - Error (Status 500)"
response=$(curl -s -o /dev/null -w "%{http_code}" -X POST ${BASE_URL}/logs \
    -H "Content-Type: application/json" \
    -d '{
        "apiKeyId": 1,
        "serviceRouteId": 2,
        "method": "POST",
        "path": "/api/orders",
        "status": 500,
        "latencyMs": 230,
        "rateLimited": false,
        "clientIp": "192.168.1.101",
        "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'"
    }')
if [ "$response" = "202" ]; then
    print_result 0 "Send log event (error)"
else
    print_result 1 "Send log event (error) (Expected 202, got $response)"
fi
echo ""

# Test 6: Send Test Log Event (Rate Limited)
echo "Test 6: Send Test Log Event - Rate Limited"
response=$(curl -s -o /dev/null -w "%{http_code}" -X POST ${BASE_URL}/logs \
    -H "Content-Type: application/json" \
    -d '{
        "apiKeyId": 1,
        "serviceRouteId": 2,
        "method": "GET",
        "path": "/api/payments/456",
        "status": 429,
        "latencyMs": 10,
        "rateLimited": true,
        "clientIp": "192.168.1.102",
        "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'"
    }')
if [ "$response" = "202" ]; then
    print_result 0 "Send log event (rate limited)"
else
    print_result 1 "Send log event (rate limited) (Expected 202, got $response)"
fi
echo ""

# Wait for processing
echo "Waiting 2 seconds for event processing..."
sleep 2

# Test 7: Check Prometheus Metrics for Custom Counters
echo "Test 7: Check Prometheus Metrics (Custom Counters)"
metrics=$(curl -s ${BASE_URL}/actuator/prometheus | grep "nexus_requests_total")
if [ -n "$metrics" ]; then
    print_result 0 "Custom Prometheus metrics (nexus_requests_total found)"
    echo -e "${YELLOW}Sample metrics:${NC}"
    echo "$metrics" | head -n 3
else
    print_result 1 "Custom Prometheus metrics (nexus_requests_total not found)"
fi
echo ""

# Test 8: Get Analytics Overview
echo "Test 8: Get Analytics Overview"
response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/analytics/overview)
http_code=$(echo "$response" | tail -n 1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" = "200" ]; then
    print_result 0 "Get analytics overview"
    echo -e "${YELLOW}Overview:${NC}"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
else
    print_result 1 "Get analytics overview (Expected 200, got $http_code)"
fi
echo ""

# Test 9: Get Recent Requests (Paginated)
echo "Test 9: Get Recent Requests (Paginated)"
response=$(curl -s -w "\n%{http_code}" "${BASE_URL}/analytics/recent-requests?page=0&size=5")
http_code=$(echo "$response" | tail -n 1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" = "200" ]; then
    print_result 0 "Get recent requests"
    echo -e "${YELLOW}Recent requests:${NC}"
    echo "$body" | jq '.content | length' 2>/dev/null || echo "$body"
else
    print_result 1 "Get recent requests (Expected 200, got $http_code)"
fi
echo ""

# Test 10: Get Top Endpoints
echo "Test 10: Get Top Endpoints"
response=$(curl -s -w "\n%{http_code}" "${BASE_URL}/analytics/top-endpoints?limit=5")
http_code=$(echo "$response" | tail -n 1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" = "200" ]; then
    print_result 0 "Get top endpoints"
    echo -e "${YELLOW}Top endpoints:${NC}"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
else
    print_result 1 "Get top endpoints (Expected 200, got $http_code)"
fi
echo ""

# Test 11: Send Multiple Log Events (Load Test)
echo "Test 11: Send 10 Log Events (Load Test)"
success_count=0
for i in {1..10}; do
    response=$(curl -s -o /dev/null -w "%{http_code}" -X POST ${BASE_URL}/logs \
        -H "Content-Type: application/json" \
        -d '{
            "apiKeyId": 1,
            "serviceRouteId": '"$((i % 3 + 1))"',
            "method": "GET",
            "path": "/api/test/'"$i"'",
            "status": '"$((i % 2 == 0 ? 200 : 500))"',
            "latencyMs": '"$((RANDOM % 500 + 50))"',
            "rateLimited": '"$([ $((i % 5)) -eq 0 ] && echo "true" || echo "false")"',
            "clientIp": "192.168.1.'"$((100 + i))"'",
            "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'"
        }')
    if [ "$response" = "202" ]; then
        success_count=$((success_count + 1))
    fi
done
if [ "$success_count" -eq 10 ]; then
    print_result 0 "Load test (10/10 events accepted)"
else
    print_result 1 "Load test ($success_count/10 events accepted)"
fi
echo ""

# Test 12: Invalid Request (Missing Required Field)
echo "Test 12: Invalid Request (Missing method field)"
response=$(curl -s -o /dev/null -w "%{http_code}" -X POST ${BASE_URL}/logs \
    -H "Content-Type: application/json" \
    -d '{
        "apiKeyId": 1,
        "serviceRouteId": 2,
        "path": "/api/invalid",
        "status": 200,
        "latencyMs": 100,
        "rateLimited": false,
        "clientIp": "192.168.1.200",
        "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'"
    }')
if [ "$response" = "400" ]; then
    print_result 0 "Invalid request validation (Expected 400)"
else
    print_result 1 "Invalid request validation (Expected 400, got $response)"
fi
echo ""

# Summary
echo "=================================="
echo "Test Summary"
echo "=================================="
echo ""
echo "All tests completed!"
echo ""
echo "Next Steps:"
echo "1. Check database: SELECT COUNT(*) FROM request_logs;"
echo "2. View Prometheus metrics: curl ${BASE_URL}/actuator/prometheus | grep nexus_"
echo "3. Check analytics overview: curl ${BASE_URL}/analytics/overview | jq '.'"
echo "4. Monitor scheduled aggregation job (runs daily at 2 AM)"
echo ""
