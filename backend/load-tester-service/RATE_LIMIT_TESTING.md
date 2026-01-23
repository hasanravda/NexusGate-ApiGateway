# Rate Limit Testing Guide for NexusGate API Gateway

## Overview

This comprehensive guide provides tools and examples for **testing and validating API Gateway rate limiting** behavior in the NexusGate microservices architecture.

## Architecture Context

### System Components

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Rate Limit Testing Flow                          │
└─────────────────────────────────────────────────────────────────────┘

    Load Tester Service (Port 8083)
            │
            │ Sends concurrent requests
            │ with same API-Key
            ▼
    API Gateway (Port 8081)
            │
            ├─► Rate Limit Filter (Redis-based)
            │   ├─ Checks API key rate limits
            │   ├─ Allows first N requests (HTTP 200)
            │   └─ Blocks excess requests (HTTP 429)
            │
            ├─► Logs all requests (allowed + blocked)
            │   └─ Sends to Analytics Service
            │
            └─► Routes allowed requests only
                └─ Backend services never see 429 traffic

    Analytics Service (Port 8085)
            │
            └─► Tracks metrics:
                ├─ Total requests
                ├─ Blocked requests (429)
                ├─ Success rate
                └─ Error rate
```

## Components Provided

### 1. RateLimitTestRunner.java
**Location:** `src/main/java/com/nexusgate/loadtest/tester/RateLimitTestRunner.java`

**Purpose:** Core testing engine that executes concurrent HTTP requests to validate rate limiting.

**Key Features:**
- ✅ Concurrent request execution using `ExecutorService`
- ✅ Thread-safe metrics collection with `LongAdder` and atomic operations
- ✅ Detailed per-request logging (timestamp, status code, latency)
- ✅ Automatic validation of rate limit behavior
- ✅ Burst testing (no delay) or controlled rate testing

**Usage Example:**
```java
@Autowired
private RateLimitTestRunner testRunner;

// Configure test
RateLimitTestConfig config = new RateLimitTestConfig(
    "http://localhost:8081/api/users",  // Target endpoint
    "nx_test_key_12345",                 // API key
    50,                                   // Total requests
    10,                                   // Concurrent threads
    20                                    // Expected rate limit
);

// Execute test
RateLimitTestResult result = testRunner.runRateLimitTest(config);

// Check results
System.out.println("Test Passed: " + result.isTestPassed());
System.out.println("Successful: " + result.getSuccessfulRequests());
System.out.println("Rate Limited: " + result.getRateLimitedRequests());
```

**Expected Output:**
```
================================================================================
Starting Rate Limit Test
================================================================================
Target Endpoint: http://localhost:8081/api/users
API Key: nx_test_ke***
Total Requests: 50
Concurrent Threads: 10
Expected Rate Limit: 20 requests
================================================================================
✓ Request #1 SUCCESS - Status: 200 | Latency: 45ms | Successful so far: 1
✓ Request #2 SUCCESS - Status: 200 | Latency: 52ms | Successful so far: 2
...
✓ Request #20 SUCCESS - Status: 200 | Latency: 38ms | Successful so far: 20
⚠ Request #21 RATE LIMITED (429) - Latency: 12ms | Blocked after 20 successful
⚠ Request #22 RATE LIMITED (429) - Latency: 8ms | Blocked after 20 successful
...
================================================================================
RATE LIMIT TEST SUMMARY
================================================================================
Test Results:
  ✓ Successful (2xx)  : 20 requests
  ⚠ Rate Limited (429): 30 requests
  ✗ Errors (5xx/other): 0 requests

Test Validation: ✓ PASSED
================================================================================
```

---

### 2. AnalyticsVerifier.java
**Location:** `src/main/java/com/nexusgate/loadtest/tester/AnalyticsVerifier.java`

**Purpose:** Verifies that the Analytics Service correctly tracked all requests from the load test.

**Key Features:**
- ✅ Fetches analytics data via REST API
- ✅ Compares analytics totals with test results
- ✅ Validates blocked request counts (429s)
- ✅ Queries recent requests and top endpoints
- ✅ Tolerance-based matching (allows small timing discrepancies)

**Usage Example:**
```java
@Autowired
private AnalyticsVerifier verifier;

// After running a test...
AnalyticsVerificationResult result = verifier.verifyAnalytics(
    "http://localhost:8085",  // Analytics URL
    testResult                 // Result from RateLimitTestRunner
);

// Check verification
System.out.println("Verified: " + result.isVerified());
System.out.println("Analytics Total: " + result.getAnalyticsTotal());
System.out.println("Analytics Blocked: " + result.getAnalyticsBlocked());
```

**Expected Output:**
```
================================================================================
Verifying Analytics Data
================================================================================
Fetching analytics from: http://localhost:8085/analytics/overview

Analytics Data Retrieved:
  Total Requests  : 50
  Blocked (429)   : 30
  Total Errors    : 30

Expected from Load Test:
  Total Requests  : 50
  Rate Limited    : 30

✓ Verification Result: PASSED

================================================================================
ANALYTICS VERIFICATION SUMMARY
================================================================================
Status: ✓ PASSED

Comparison:
  Metric              | Expected | Analytics | Match
  ------------------------------------------------------------
  Total Requests      |       50 |        50 | ✓
  Blocked Requests    |       30 |        30 | ✓

================================================================================
```

---

### 3. RateLimitTestController.java
**Location:** `src/main/java/com/nexusgate/loadtest/controller/RateLimitTestController.java`

**Purpose:** REST API endpoints for executing rate limit tests remotely.

**Endpoints:**

#### Execute Rate Limit Test
```bash
POST /rate-limit-test/execute
Content-Type: application/json

{
  "targetEndpoint": "http://localhost:8081/api/users",
  "apiKey": "nx_test_key_12345",
  "totalRequests": 50,
  "concurrentThreads": 10,
  "expectedRateLimit": 20,
  "delayBetweenRequestsMs": 0
}
```

**Response:**
```json
{
  "testPassed": true,
  "totalRequests": 50,
  "successfulRequests": 20,
  "rateLimitedRequests": 30,
  "errorRequests": 0,
  "unauthorizedRequests": 0,
  "durationMs": 1250,
  "requestsPerSecond": 40.0,
  "rateLimitViolations": [
    "Rate limit exceeded at request #21 (after 20 successful requests)",
    "Rate limit exceeded at request #22 (after 20 successful requests)"
  ],
  "message": "Rate limit test completed successfully"
}
```

#### Verify Analytics
```bash
POST /rate-limit-test/verify-analytics
Content-Type: application/json

{
  "analyticsBaseUrl": "http://localhost:8085"
}
```

**Response:**
```json
{
  "verified": true,
  "message": "✓ Analytics verification PASSED - All metrics match",
  "analyticsTotal": 50,
  "analyticsBlocked": 30,
  "expectedTotal": 50,
  "expectedBlocked": 30,
  "totalMatches": true,
  "blockedMatches": true
}
```

#### Full Test (Test + Analytics)
```bash
POST /rate-limit-test/full-test
Content-Type: application/json

{
  "targetEndpoint": "http://localhost:8081/api/users",
  "apiKey": "nx_test_key_12345",
  "totalRequests": 50,
  "concurrentThreads": 10,
  "expectedRateLimit": 20,
  "delayBetweenRequestsMs": 0,
  "analyticsBaseUrl": "http://localhost:8085",
  "waitForAnalyticsMs": 2000
}
```

**Response:**
```json
{
  "rateLimitTest": {
    "testPassed": true,
    "totalRequests": 50,
    "successfulRequests": 20,
    "rateLimitedRequests": 30,
    "errorRequests": 0,
    "durationMs": 1250,
    "requestsPerSecond": 40.0
  },
  "analyticsVerification": {
    "verified": true,
    "message": "✓ Analytics verification PASSED",
    "analyticsTotal": 50,
    "analyticsBlocked": 30,
    "totalMatches": true,
    "blockedMatches": true
  },
  "overallStatus": "PASSED",
  "message": "✓ Full rate limit test PASSED - Test and analytics verification successful"
}
```

---

### 4. test-rate-limiting.sh
**Location:** `load-tester-service/test-rate-limiting.sh`

**Purpose:** Automated bash script for end-to-end rate limit testing.

**Features:**
- ✅ Pre-flight service health checks
- ✅ Execute rate limit test
- ✅ Verify analytics tracking
- ✅ Query recent requests and top endpoints
- ✅ Colored output and detailed reporting
- ✅ Exit codes for CI/CD integration

**Usage:**
```bash
# Make executable
chmod +x test-rate-limiting.sh

# Run test
./test-rate-limiting.sh

# Or with custom parameters
TOTAL_REQUESTS=100 EXPECTED_RATE_LIMIT=50 ./test-rate-limiting.sh
```

**Expected Output:**
```
========================================================================
PRE-FLIGHT CHECKS
========================================================================

➜ Checking if all required services are running...
✓ API Gateway is running (http://localhost:8081/actuator/health)
✓ Load Tester Service is running (http://localhost:8083/load-test/health)
✓ Analytics Service is running (http://localhost:8085/actuator/health)
✓ All services are running!

========================================================================
TEST SCENARIO 1: Basic Rate Limit Validation
========================================================================

➜ Configuration:
  Target Endpoint      : http://localhost:8081/api/users
  API Key              : nx_test_ke***
  Total Requests       : 50
  Concurrent Threads   : 10
  Expected Rate Limit  : 20 requests

➜ Executing rate limit test...

➜ Test Results:
  Total Requests       : 50
  ✓ Successful (2xx)   : 20
  ⚠ Rate Limited (429) : 30
  ✗ Errors (5xx/other) : 0
  ✗ Unauthorized       : 0

  Duration             : 1250ms (1.25s)
  Throughput           : 40.00 req/s

✓ Rate Limit Test: PASSED

  Expected Behavior Confirmed:
  ✓ First 20 requests succeeded (within rate limit)
  ✓ Next 30 requests were blocked with HTTP 429
  ✓ Rate limiting is working correctly

========================================================================
TEST SCENARIO 2: Analytics Verification
========================================================================

➜ Waiting 3 seconds for analytics to process...
➜ Verifying analytics data...

➜ Analytics Comparison:

  Metric              | Expected | Analytics | Match
  ---------------------------------------------------------
  Total Requests      |       50 |        50 | ✓
  Blocked Requests    |       30 |        30 | ✓

✓ Analytics Verification: PASSED

  Confirmed:
  ✓ All requests (allowed + blocked) were logged
  ✓ Rate-limited requests (429) correctly tracked as errors
  ✓ Analytics data matches load test results

========================================================================
FINAL TEST SUMMARY
========================================================================

Test Results:

✓ ✓ Rate Limit Test: PASSED
✓ ✓ Analytics Verification: PASSED

Key Findings:
  • First 20 requests succeeded (within rate limit)
  • Next 30 requests were blocked with HTTP 429
  • No requests bypassed rate limiting to reach backend
  • Analytics tracked 50 total requests
  • Analytics tracked 30 blocked requests

==========================================
   ALL TESTS PASSED - Rate limiting is
   working correctly and analytics are
   properly tracking requests!
==========================================
```

---

## Test Scenarios

### Scenario 1: Basic Rate Limit Validation

**Goal:** Verify rate limits are enforced correctly

```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/api/users",
    "apiKey": "nx_test_key_12345",
    "totalRequests": 50,
    "concurrentThreads": 10,
    "expectedRateLimit": 20
  }'
```

**Expected Behavior:**
- ✅ First 20 requests return HTTP 200 (allowed)
- ✅ Remaining 30 requests return HTTP 429 (rate limited)
- ✅ No 401/403 authorization errors
- ✅ Blocked requests do NOT reach backend services

---

### Scenario 2: High Concurrency Burst Test

**Goal:** Test rate limiting under burst traffic

```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/api/users",
    "apiKey": "nx_test_key_12345",
    "totalRequests": 100,
    "concurrentThreads": 50,
    "expectedRateLimit": 20,
    "delayBetweenRequestsMs": 0
  }'
```

**Expected Behavior:**
- ✅ Rate limiting handles burst correctly
- ✅ First ~20 requests allowed
- ✅ ~80 requests blocked with 429
- ✅ No race conditions or missed limits

---

### Scenario 3: Controlled Rate Test

**Goal:** Test with gradual request rate

```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/api/users",
    "apiKey": "nx_test_key_12345",
    "totalRequests": 30,
    "concurrentThreads": 5,
    "expectedRateLimit": 20,
    "delayBetweenRequestsMs": 100
  }'
```

**Expected Behavior:**
- ✅ Requests spread over time
- ✅ Rate limit window respected
- ✅ Smoother distribution of allowed/blocked requests

---

### Scenario 4: Full End-to-End Test

**Goal:** Test rate limiting + analytics verification

```bash
curl -X POST http://localhost:8083/rate-limit-test/full-test \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/api/users",
    "apiKey": "nx_test_key_12345",
    "totalRequests": 50,
    "concurrentThreads": 10,
    "expectedRateLimit": 20,
    "analyticsBaseUrl": "http://localhost:8085",
    "waitForAnalyticsMs": 2000
  }'
```

**Expected Behavior:**
- ✅ Rate limit test passes
- ✅ Analytics verification passes
- ✅ Total request counts match
- ✅ Blocked request counts match

---

## Key Validations Performed

### 1. Rate Limit Enforcement
- ✅ Requests within limit receive HTTP 200
- ✅ Requests exceeding limit receive HTTP 429
- ✅ Rate limit threshold matches configuration
- ✅ No requests bypass rate limiting

### 2. Request Logging
- ✅ All requests (200 + 429) are logged
- ✅ Timestamp accuracy
- ✅ Endpoint path recorded
- ✅ API key tracked
- ✅ Status code captured

### 3. Analytics Accuracy
- ✅ Total request count matches test
- ✅ Blocked request count (429) matches
- ✅ Error rate calculated correctly
- ✅ Recent requests queryable
- ✅ Top endpoints tracked

### 4. Backend Protection
- ✅ Rate-limited requests (429) do NOT reach backend
- ✅ Only allowed requests forwarded
- ✅ Backend never receives excess traffic

---

## Troubleshooting

### Issue: All Requests Return 401 Unauthorized

**Cause:** Invalid or inactive API key

**Solution:**
```bash
# Check API key in database
psql -U nexusgate -d nexusgate -c "SELECT key_value, is_active FROM api_keys WHERE key_value LIKE 'nx_test%';"

# Use a valid, active API key from the database
```

---

### Issue: No Rate Limiting (All Requests Succeed)

**Cause:** Rate limiting not configured or disabled

**Solution:**
```sql
-- Check if route has rate limiting enabled
SELECT id, path, rate_limit_enabled FROM service_routes WHERE path LIKE '%/api/users%';

-- Enable rate limiting for route
UPDATE service_routes SET rate_limit_enabled = true WHERE path = '/api/users/**';
```

---

### Issue: Analytics Verification Fails (Count Mismatch)

**Cause:** Timing issues or log processing delay

**Solution:**
```bash
# Increase wait time for analytics processing
curl -X POST http://localhost:8083/rate-limit-test/full-test \
  -d '{"waitForAnalyticsMs": 5000, ...}'

# Or verify manually after delay
sleep 5
curl http://localhost:8085/analytics/overview
```

---

### Issue: High Error Count (5xx)

**Cause:** Backend service issues or gateway configuration

**Solution:**
```bash
# Check backend service health
curl http://localhost:8082/actuator/health

# Check gateway logs
tail -f /tmp/gateway.log

# Verify service route configuration
```

---

## CI/CD Integration

### Using Bash Script
```bash
# In CI pipeline
./test-rate-limiting.sh
EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
  echo "Rate limit tests PASSED"
else
  echo "Rate limit tests FAILED"
  exit 1
fi
```

### Using REST API
```bash
# Execute test
RESPONSE=$(curl -s -X POST http://localhost:8083/rate-limit-test/full-test -d '...')

# Check status
TEST_PASSED=$(echo "$RESPONSE" | jq -r '.rateLimitTest.testPassed')
ANALYTICS_VERIFIED=$(echo "$RESPONSE" | jq -r '.analyticsVerification.verified')

if [ "$TEST_PASSED" == "true" ] && [ "$ANALYTICS_VERIFIED" == "true" ]; then
  echo "✓ All tests passed"
  exit 0
else
  echo "✗ Tests failed"
  exit 1
fi
```

---

## Summary

This comprehensive testing framework provides:

✅ **Production-ready** rate limit validation  
✅ **Thread-safe** concurrent testing  
✅ **Detailed logging** of all requests  
✅ **Analytics verification** to ensure data accuracy  
✅ **Automated testing** via bash scripts  
✅ **REST API** for remote execution  
✅ **CI/CD ready** with proper exit codes

**Next Steps:**
1. Run `./test-rate-limiting.sh` to validate your setup
2. Customize test parameters for your rate limits
3. Integrate into your CI/CD pipeline
4. Monitor analytics dashboards for production traffic

