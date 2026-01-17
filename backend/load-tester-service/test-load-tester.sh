#!/bin/bash

# Load Testing Service - Test Script
# This script demonstrates how to use the load testing service

BASE_URL="http://localhost:8083"
TARGET_ENDPOINT="http://localhost:8080/api/users"
API_KEY="nx_test_key_123"

echo "=========================================="
echo "Load Testing Service - Test Script"
echo "=========================================="
echo ""

# Color codes for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}➜ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if service is running
echo "1. Checking service health..."
HEALTH_RESPONSE=$(curl -s -w "%{http_code}" -o /tmp/health.json $BASE_URL/load-test/health)
HTTP_CODE=${HEALTH_RESPONSE: -3}

if [ "$HTTP_CODE" == "200" ]; then
    print_success "Service is healthy"
    cat /tmp/health.json | jq '.'
else
    print_error "Service is not responding (HTTP $HTTP_CODE)"
    exit 1
fi

echo ""
echo "=========================================="
echo "2. Starting Load Test - Constant Rate"
echo "=========================================="

# Start a load test
TEST_REQUEST='{
  "targetKey": "'$API_KEY'",
  "targetEndpoint": "'$TARGET_ENDPOINT'",
  "requestRate": 50,
  "durationSeconds": 30,
  "concurrencyLevel": 5,
  "requestPattern": "CONSTANT_RATE",
  "httpMethod": "GET"
}'

print_info "Sending test request..."
echo "$TEST_REQUEST" | jq '.'

START_RESPONSE=$(curl -s -X POST $BASE_URL/load-test/start \
    -H "Content-Type: application/json" \
    -d "$TEST_REQUEST")

echo ""
print_info "Response:"
echo "$START_RESPONSE" | jq '.'

# Extract test ID
TEST_ID=$(echo "$START_RESPONSE" | jq -r '.testId')

if [ "$TEST_ID" == "null" ] || [ -z "$TEST_ID" ]; then
    print_error "Failed to start test"
    exit 1
fi

print_success "Test started with ID: $TEST_ID"

echo ""
echo "=========================================="
echo "3. Monitoring Test Progress"
echo "=========================================="

# Monitor test progress
DURATION=30
INTERVAL=5
ELAPSED=0

while [ $ELAPSED -lt $DURATION ]; do
    sleep $INTERVAL
    ELAPSED=$((ELAPSED + INTERVAL))
    
    print_info "Status at ${ELAPSED}s..."
    
    STATUS_RESPONSE=$(curl -s $BASE_URL/load-test/status/$TEST_ID)
    
    # Extract key metrics
    STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.status')
    TOTAL=$(echo "$STATUS_RESPONSE" | jq -r '.totalRequests // 0')
    SUCCESS=$(echo "$STATUS_RESPONSE" | jq -r '.successfulRequests // 0')
    RATE_LIMITED=$(echo "$STATUS_RESPONSE" | jq -r '.rateLimitedRequests // 0')
    AVG_LATENCY=$(echo "$STATUS_RESPONSE" | jq -r '.averageLatencyMs // 0')
    RPS=$(echo "$STATUS_RESPONSE" | jq -r '.requestsPerSecond // 0')
    
    echo "  Status: $STATUS"
    echo "  Total Requests: $TOTAL"
    echo "  Successful: $SUCCESS"
    echo "  Rate Limited: $RATE_LIMITED"
    echo "  Avg Latency: ${AVG_LATENCY}ms"
    echo "  Current RPS: $RPS"
    echo ""
done

echo "=========================================="
echo "4. Retrieving Final Results"
echo "=========================================="

# Wait a bit more for test completion
sleep 5

print_info "Fetching final results..."
RESULTS=$(curl -s $BASE_URL/load-test/results/$TEST_ID)

echo "$RESULTS" | jq '.'

# Print summary
echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="

STATUS=$(echo "$RESULTS" | jq -r '.status')
TOTAL=$(echo "$RESULTS" | jq -r '.totalRequests')
SUCCESS=$(echo "$RESULTS" | jq -r '.successfulRequests')
RATE_LIMITED=$(echo "$RESULTS" | jq -r '.rateLimitedRequests')
ERRORS=$(echo "$RESULTS" | jq -r '.errorRequests')
SUCCESS_RATE=$(echo "$RESULTS" | jq -r '.successRate')
RATE_LIMIT_RATE=$(echo "$RESULTS" | jq -r '.rateLimitRate')
AVG_LATENCY=$(echo "$RESULTS" | jq -r '.averageLatencyMs')
P95_LATENCY=$(echo "$RESULTS" | jq -r '.p95LatencyMs')
MIN_LATENCY=$(echo "$RESULTS" | jq -r '.minLatencyMs')
MAX_LATENCY=$(echo "$RESULTS" | jq -r '.maxLatencyMs')
THROUGHPUT=$(echo "$RESULTS" | jq -r '.requestsPerSecond')

echo "Test ID: $TEST_ID"
echo "Status: $STATUS"
echo ""
echo "Request Statistics:"
echo "  Total Requests: $TOTAL"
echo "  Successful (2xx): $SUCCESS (${SUCCESS_RATE}%)"
echo "  Rate Limited (429): $RATE_LIMITED (${RATE_LIMIT_RATE}%)"
echo "  Errors: $ERRORS"
echo ""
echo "Latency Statistics:"
echo "  Average: ${AVG_LATENCY}ms"
echo "  P95: ${P95_LATENCY}ms"
echo "  Min: ${MIN_LATENCY}ms"
echo "  Max: ${MAX_LATENCY}ms"
echo ""
echo "Performance:"
echo "  Throughput: ${THROUGHPUT} req/s"
echo ""

if [ "$STATUS" == "COMPLETED" ]; then
    print_success "Test completed successfully!"
else
    print_error "Test finished with status: $STATUS"
fi

echo ""
echo "=========================================="
echo "5. Additional Test Scenarios (Optional)"
echo "=========================================="

read -p "Run burst load test? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_info "Starting burst load test..."
    
    BURST_REQUEST='{
      "targetKey": "'$API_KEY'",
      "targetEndpoint": "'$TARGET_ENDPOINT'",
      "requestRate": 200,
      "durationSeconds": 10,
      "concurrencyLevel": 20,
      "requestPattern": "BURST",
      "httpMethod": "GET"
    }'
    
    BURST_RESPONSE=$(curl -s -X POST $BASE_URL/load-test/start \
        -H "Content-Type: application/json" \
        -d "$BURST_REQUEST")
    
    BURST_TEST_ID=$(echo "$BURST_RESPONSE" | jq -r '.testId')
    print_success "Burst test started: $BURST_TEST_ID"
    
    echo "Monitor with: curl $BASE_URL/load-test/status/$BURST_TEST_ID"
fi

echo ""
echo "=========================================="
echo "Test Script Completed"
echo "=========================================="
echo ""
echo "Useful commands:"
echo "  - Check status: curl $BASE_URL/load-test/status/$TEST_ID | jq"
echo "  - Get results:  curl $BASE_URL/load-test/results/$TEST_ID | jq"
echo "  - Stop test:    curl -X DELETE $BASE_URL/load-test/stop/$TEST_ID"
echo ""
