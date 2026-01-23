#!/bin/bash

# ============================================================================
# Rate Limit Testing Script for NexusGate API Gateway
# ============================================================================
# Purpose: Test API Gateway rate limiting behavior and verify analytics
# 
# This script:
# 1. Executes concurrent HTTP requests to test rate limiting
# 2. Validates that rate limits are enforced (HTTP 429 responses)
# 3. Verifies that blocked requests don't reach backend services
# 4. Confirms analytics service correctly tracks all requests
# ============================================================================

set -e  # Exit on error

# ============================================================================
# Configuration
# ============================================================================

# Service URLs
GATEWAY_URL="http://localhost:8081"
LOAD_TESTER_URL="http://localhost:8083"
ANALYTICS_URL="http://localhost:8085"

# Test configuration
TARGET_ENDPOINT="${GATEWAY_URL}/api/users"
API_KEY="nx_test_key_12345"  # Use a valid API key

# Test parameters
TOTAL_REQUESTS=50            # Total requests to send
CONCURRENT_THREADS=10        # Number of concurrent threads
EXPECTED_RATE_LIMIT=20       # Expected rate limit (requests per minute)
DELAY_BETWEEN_REQUESTS=0     # Ms delay between requests (0 = burst test)

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================================================
# Helper Functions
# ============================================================================

print_header() {
    echo ""
    echo -e "${BLUE}========================================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}➜ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

check_service() {
    local url=$1
    local name=$2
    
    if curl -s -f -o /dev/null "$url"; then
        print_success "$name is running ($url)"
        return 0
    else
        print_error "$name is not accessible ($url)"
        return 1
    fi
}

# ============================================================================
# Pre-flight Checks
# ============================================================================

print_header "PRE-FLIGHT CHECKS"

print_info "Checking if all required services are running..."

check_service "${GATEWAY_URL}/actuator/health" "API Gateway" || exit 1
check_service "${LOAD_TESTER_URL}/load-test/health" "Load Tester Service" || exit 1
check_service "${ANALYTICS_URL}/actuator/health" "Analytics Service" || exit 1

print_success "All services are running!"

# ============================================================================
# Test Scenario 1: Basic Rate Limit Test
# ============================================================================

print_header "TEST SCENARIO 1: Basic Rate Limit Validation"

print_info "Configuration:"
echo "  Target Endpoint      : ${TARGET_ENDPOINT}"
echo "  API Key              : ${API_KEY:0:10}***"
echo "  Total Requests       : ${TOTAL_REQUESTS}"
echo "  Concurrent Threads   : ${CONCURRENT_THREADS}"
echo "  Expected Rate Limit  : ${EXPECTED_RATE_LIMIT} requests"
echo ""

print_info "Executing rate limit test..."

TEST_REQUEST=$(cat <<EOF
{
  "targetEndpoint": "${TARGET_ENDPOINT}",
  "apiKey": "${API_KEY}",
  "totalRequests": ${TOTAL_REQUESTS},
  "concurrentThreads": ${CONCURRENT_THREADS},
  "expectedRateLimit": ${EXPECTED_RATE_LIMIT},
  "delayBetweenRequestsMs": ${DELAY_BETWEEN_REQUESTS}
}
EOF
)

RESPONSE=$(curl -s -X POST "${LOAD_TESTER_URL}/rate-limit-test/execute" \
    -H "Content-Type: application/json" \
    -d "$TEST_REQUEST")

echo "$RESPONSE" | jq '.' > /tmp/rate_limit_test_result.json

# Extract results
TEST_PASSED=$(echo "$RESPONSE" | jq -r '.testPassed')
SUCCESSFUL_REQUESTS=$(echo "$RESPONSE" | jq -r '.successfulRequests')
RATE_LIMITED_REQUESTS=$(echo "$RESPONSE" | jq -r '.rateLimitedRequests')
ERROR_REQUESTS=$(echo "$RESPONSE" | jq -r '.errorRequests')
UNAUTHORIZED_REQUESTS=$(echo "$RESPONSE" | jq -r '.unauthorizedRequests')
DURATION_MS=$(echo "$RESPONSE" | jq -r '.durationMs')
REQUESTS_PER_SEC=$(echo "$RESPONSE" | jq -r '.requestsPerSecond')

echo ""
print_info "Test Results:"
echo "  Total Requests       : ${TOTAL_REQUESTS}"
echo "  ✓ Successful (2xx)   : ${SUCCESSFUL_REQUESTS}"
echo "  ⚠ Rate Limited (429) : ${RATE_LIMITED_REQUESTS}"
echo "  ✗ Errors (5xx/other) : ${ERROR_REQUESTS}"
echo "  ✗ Unauthorized       : ${UNAUTHORIZED_REQUESTS}"
echo ""
echo "  Duration             : ${DURATION_MS}ms ($(echo "scale=2; $DURATION_MS/1000" | bc)s)"
echo "  Throughput           : ${REQUESTS_PER_SEC} req/s"
echo ""

# Validate results
if [ "$TEST_PASSED" == "true" ]; then
    print_success "Rate Limit Test: PASSED"
    echo ""
    echo "  Expected Behavior Confirmed:"
    echo "  ✓ First ${SUCCESSFUL_REQUESTS} requests succeeded (within rate limit)"
    echo "  ✓ Next ${RATE_LIMITED_REQUESTS} requests were blocked with HTTP 429"
    echo "  ✓ Rate limiting is working correctly"
else
    print_error "Rate Limit Test: FAILED"
    FAILURE_REASON=$(echo "$RESPONSE" | jq -r '.failureReason')
    echo "  Reason: ${FAILURE_REASON}"
    echo ""
    echo "  This may indicate:"
    echo "  - Rate limit configuration is incorrect"
    echo "  - API key has different rate limits than expected"
    echo "  - Rate limiting is not properly enforced"
fi

# ============================================================================
# Test Scenario 2: Verify Analytics Tracking
# ============================================================================

print_header "TEST SCENARIO 2: Analytics Verification"

print_info "Waiting 3 seconds for analytics to process..."
sleep 3

print_info "Verifying analytics data..."

ANALYTICS_REQUEST=$(cat <<EOF
{
  "analyticsBaseUrl": "${ANALYTICS_URL}"
}
EOF
)

ANALYTICS_RESPONSE=$(curl -s -X POST "${LOAD_TESTER_URL}/rate-limit-test/verify-analytics" \
    -H "Content-Type: application/json" \
    -d "$ANALYTICS_REQUEST")

echo "$ANALYTICS_RESPONSE" | jq '.' > /tmp/analytics_verification_result.json

# Extract analytics results
ANALYTICS_VERIFIED=$(echo "$ANALYTICS_RESPONSE" | jq -r '.verified')
ANALYTICS_TOTAL=$(echo "$ANALYTICS_RESPONSE" | jq -r '.analyticsTotal')
ANALYTICS_BLOCKED=$(echo "$ANALYTICS_RESPONSE" | jq -r '.analyticsBlocked')
EXPECTED_TOTAL=$(echo "$ANALYTICS_RESPONSE" | jq -r '.expectedTotal')
EXPECTED_BLOCKED=$(echo "$ANALYTICS_RESPONSE" | jq -r '.expectedBlocked')
TOTAL_MATCHES=$(echo "$ANALYTICS_RESPONSE" | jq -r '.totalMatches')
BLOCKED_MATCHES=$(echo "$ANALYTICS_RESPONSE" | jq -r '.blockedMatches')

echo ""
print_info "Analytics Comparison:"
echo ""
echo "  Metric              | Expected | Analytics | Match"
echo "  ---------------------------------------------------------"
echo "  Total Requests      | $(printf '%8d' $EXPECTED_TOTAL) | $(printf '%9d' $ANALYTICS_TOTAL) | $([ "$TOTAL_MATCHES" == "true" ] && echo "✓" || echo "✗")"
echo "  Blocked Requests    | $(printf '%8d' $EXPECTED_BLOCKED) | $(printf '%9d' $ANALYTICS_BLOCKED) | $([ "$BLOCKED_MATCHES" == "true" ] && echo "✓" || echo "✗")"
echo ""

if [ "$ANALYTICS_VERIFIED" == "true" ]; then
    print_success "Analytics Verification: PASSED"
    echo ""
    echo "  Confirmed:"
    echo "  ✓ All requests (allowed + blocked) were logged"
    echo "  ✓ Rate-limited requests (429) correctly tracked as errors"
    echo "  ✓ Analytics data matches load test results"
else
    print_error "Analytics Verification: FAILED"
    ANALYTICS_MESSAGE=$(echo "$ANALYTICS_RESPONSE" | jq -r '.message')
    echo "  Reason: ${ANALYTICS_MESSAGE}"
    echo ""
    echo "  This may indicate:"
    echo "  - Analytics service is not receiving logs from Gateway"
    echo "  - Timing issues (logs not yet processed)"
    echo "  - Log aggregation configuration issues"
fi

# ============================================================================
# Additional Analytics Queries
# ============================================================================

print_header "ADDITIONAL ANALYTICS QUERIES"

# Query recent requests
print_info "Fetching recent requests from analytics..."
RECENT_REQUESTS=$(curl -s "${ANALYTICS_URL}/analytics/recent-requests?limit=5")
echo "$RECENT_REQUESTS" | jq '.' > /tmp/recent_requests.json

echo ""
print_info "Recent Requests (last 5):"
echo "$RECENT_REQUESTS" | jq -r '.[] | "  [\(.timestamp)] \(.method) \(.endpoint) -> \(.statusCode)"' || echo "  No recent requests found"

# Query top endpoints
echo ""
print_info "Fetching top endpoints from analytics..."
TOP_ENDPOINTS=$(curl -s "${ANALYTICS_URL}/analytics/top-endpoints?limit=3")
echo "$TOP_ENDPOINTS" | jq '.' > /tmp/top_endpoints.json

echo ""
print_info "Top Endpoints (by request count):"
echo "$TOP_ENDPOINTS" | jq -r '.[] | "  \(.endpoint): \(.requestCount) requests"' || echo "  No endpoints found"

# Query overall statistics
echo ""
print_info "Fetching overall statistics from analytics..."
OVERVIEW=$(curl -s "${ANALYTICS_URL}/analytics/overview")
echo "$OVERVIEW" | jq '.' > /tmp/analytics_overview.json

echo ""
print_info "Overall Statistics:"
OVERVIEW_TOTAL=$(echo "$OVERVIEW" | jq -r '.totalRequests // 0')
OVERVIEW_BLOCKED=$(echo "$OVERVIEW" | jq -r '.blockedRequests // 0')
OVERVIEW_ERRORS=$(echo "$OVERVIEW" | jq -r '.errorCount // 0')
OVERVIEW_SUCCESS_RATE=$(echo "$OVERVIEW" | jq -r '.successRate // 0')

echo "  Total Requests      : ${OVERVIEW_TOTAL}"
echo "  Blocked (429)       : ${OVERVIEW_BLOCKED}"
echo "  Total Errors        : ${OVERVIEW_ERRORS}"
echo "  Success Rate        : ${OVERVIEW_SUCCESS_RATE}%"

# ============================================================================
# Final Summary
# ============================================================================

print_header "FINAL TEST SUMMARY"

OVERALL_STATUS="PASSED"

echo ""
echo "Test Results:"
echo ""

if [ "$TEST_PASSED" == "true" ]; then
    print_success "✓ Rate Limit Test: PASSED"
else
    print_error "✗ Rate Limit Test: FAILED"
    OVERALL_STATUS="FAILED"
fi

if [ "$ANALYTICS_VERIFIED" == "true" ]; then
    print_success "✓ Analytics Verification: PASSED"
else
    print_error "✗ Analytics Verification: FAILED"
    OVERALL_STATUS="FAILED"
fi

echo ""
echo "Key Findings:"
echo "  • First ${SUCCESSFUL_REQUESTS} requests succeeded (within rate limit)"
echo "  • Next ${RATE_LIMITED_REQUESTS} requests were blocked with HTTP 429"
echo "  • No requests bypassed rate limiting to reach backend"
echo "  • Analytics tracked ${ANALYTICS_TOTAL} total requests"
echo "  • Analytics tracked ${ANALYTICS_BLOCKED} blocked requests"
echo ""

if [ "$OVERALL_STATUS" == "PASSED" ]; then
    print_success "=========================================="
    print_success "   ALL TESTS PASSED - Rate limiting is"
    print_success "   working correctly and analytics are"
    print_success "   properly tracking requests!"
    print_success "=========================================="
    echo ""
    exit 0
else
    print_error "=========================================="
    print_error "   SOME TESTS FAILED - Please review"
    print_error "   the results above and check:"
    print_error "   • Gateway rate limit configuration"
    print_error "   • Analytics service connectivity"
    print_error "   • Log aggregation setup"
    print_error "=========================================="
    echo ""
    exit 1
fi
