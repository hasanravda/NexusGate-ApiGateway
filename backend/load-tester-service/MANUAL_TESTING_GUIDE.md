# Manual Testing Guide - Rate Limiting Framework

## Prerequisites Check

```bash
# Check all services are running
curl -s http://localhost:8081/actuator/health | jq '.status'  # Gateway
curl -s http://localhost:8083/actuator/health | jq '.status'  # Load Tester
curl -s http://localhost:8085/actuator/health | jq '.status'  # Analytics
```

---

## Test 1: Simple Request Test (No Rate Limiting)

Test the framework works with a basic endpoint:

```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/actuator/health",
    "apiKey": "",
    "totalRequests": 10,
    "concurrentThreads": 3,
    "expectedRateLimit": 100
  }' | jq '.'
```

**Expected Output:**
```json
{
  "testPassed": true,
  "totalRequests": 10,
  "successfulRequests": 10,
  "rateLimitedRequests": 0,
  "errorRequests": 0,
  "durationMs": 100,
  "message": "Rate limit test completed successfully"
}
```

---

## Test 2: Burst Traffic Test

Send many requests quickly to see throughput:

```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/actuator/health",
    "apiKey": "",
    "totalRequests": 50,
    "concurrentThreads": 10,
    "expectedRateLimit": 100,
    "delayBetweenRequestsMs": 0
  }' | jq '{
    totalRequests,
    successfulRequests,
    durationMs,
    requestsPerSecond
  }'
```

**What to observe:**
- All requests should succeed (no rate limiting on health endpoint)
- Check throughput (requests/second)
- Verify concurrent execution is working

---

## Test 3: Test with Mock Backend Service

The mock backend service should be available. Let's test it directly first:

```bash
# Test mock backend directly
curl -s http://localhost:8091/api/users | jq '.'
```

If that works, test through the gateway (if route exists):

```bash
# Test through gateway
curl -s http://localhost:8081/api/users \
  -H "X-API-Key: your-api-key-here" | jq '.'
```

---

## Test 4: Manual Rate Limiting Simulation

To manually test rate limiting behavior, you can:

### Option A: Use the existing Load Test Service

Send controlled bursts:

```bash
# Send 20 requests with 5 concurrent threads
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/actuator/health",
    "apiKey": "",
    "totalRequests": 20,
    "concurrentThreads": 5,
    "expectedRateLimit": 15,
    "delayBetweenRequestsMs": 0
  }' | jq '{successfulRequests, rateLimitedRequests, errorRequests}'
```

### Option B: Manual curl loop

Send requests in a loop and count responses:

```bash
#!/bin/bash
ENDPOINT="http://localhost:8081/actuator/health"
SUCCESS=0
ERRORS=0

for i in {1..20}; do
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $ENDPOINT)
  
  if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✓ Request $i: $HTTP_CODE (Success)"
    ((SUCCESS++))
  else
    echo "✗ Request $i: $HTTP_CODE (Failed)"
    ((ERRORS++))
  fi
  
  sleep 0.1  # Small delay between requests
done

echo ""
echo "Results: $SUCCESS successful, $ERRORS failed"
```

---

## Test 5: Test Different Concurrency Levels

### Low Concurrency (Sequential-like)
```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/actuator/health",
    "apiKey": "",
    "totalRequests": 20,
    "concurrentThreads": 2,
    "expectedRateLimit": 50
  }' | jq '{durationMs, requestsPerSecond}'
```

### High Concurrency (Burst)
```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/actuator/health",
    "apiKey": "",
    "totalRequests": 20,
    "concurrentThreads": 20,
    "expectedRateLimit": 50
  }' | jq '{durationMs, requestsPerSecond}'
```

**Compare:** High concurrency should complete faster due to parallel execution.

---

## Test 6: Test with Delays (Controlled Rate)

Simulate gradual traffic increase:

```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/actuator/health",
    "apiKey": "",
    "totalRequests": 20,
    "concurrentThreads": 5,
    "expectedRateLimit": 50,
    "delayBetweenRequestsMs": 100
  }' | jq '{durationMs, requestsPerSecond}'
```

**Expected:** Requests spread over ~2 seconds (20 requests × 100ms delay)

---

## Test 7: Check Service Info

View available endpoints and last test results:

```bash
curl -s http://localhost:8083/rate-limit-test/info | jq '.'
```

**Output shows:**
- Available endpoints
- Last test summary (if any test was run)
- Service version

---

## Test 8: View Detailed Results

After running a test, examine the full response:

```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/actuator/health",
    "apiKey": "",
    "totalRequests": 10,
    "concurrentThreads": 3,
    "expectedRateLimit": 50
  }' | jq '.'
```

**Key fields to check:**
- `testPassed` - Overall test status
- `totalRequests` - How many were sent
- `successfulRequests` - How many got 2xx responses
- `rateLimitedRequests` - How many got 429 responses
- `errorRequests` - How many failed with 5xx
- `durationMs` - Total test duration
- `requestsPerSecond` - Throughput
- `rateLimitViolations` - List of when rate limits were hit
- `failureReason` - Why test failed (if any)

---

## Test 9: Analytics Verification (If Available)

If you've run tests and want to verify analytics:

```bash
# First run a test
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/actuator/health",
    "apiKey": "",
    "totalRequests": 25,
    "concurrentThreads": 5,
    "expectedRateLimit": 50
  }' | jq '{totalRequests, successfulRequests}' > /tmp/test_result.txt

# Wait for analytics to process
sleep 3

# Verify analytics
curl -X POST http://localhost:8083/rate-limit-test/verify-analytics \
  -H "Content-Type: application/json" \
  -d '{
    "analyticsBaseUrl": "http://localhost:8085"
  }' | jq '.'
```

---

## Test 10: Full End-to-End Test

Run test + analytics verification in one call:

```bash
curl -X POST http://localhost:8083/rate-limit-test/full-test \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/actuator/health",
    "apiKey": "",
    "totalRequests": 30,
    "concurrentThreads": 5,
    "expectedRateLimit": 50,
    "analyticsBaseUrl": "http://localhost:8085",
    "waitForAnalyticsMs": 2000
  }' | jq '.'
```

**Output includes:**
- `rateLimitTest` - Test results
- `analyticsVerification` - Analytics check results
- `overallStatus` - PASSED or FAILED

---

## Monitoring & Debugging

### Check Service Logs

```bash
# Load Tester logs
tail -f /tmp/load-tester.log

# Gateway logs
tail -f /tmp/gateway.log

# Analytics logs
tail -f /tmp/analytics-service.log
```

### Check Live Metrics

```bash
# While a test is running, check metrics
curl -s http://localhost:8083/actuator/metrics | jq '.names[]' | head -20
```

---

## Expected Behaviors

### ✅ Working Correctly:

1. **All requests succeed** when testing non-rate-limited endpoints
2. **Concurrent execution** completes faster than sequential
3. **Metrics are accurate** - total = successful + rate-limited + errors
4. **Thread-safe** - no race conditions, counts are correct
5. **Latency tracking** - milliseconds are reasonable (<1000ms typically)

### ⚠️ Issues to Watch For:

1. **All requests fail** → Check endpoint exists and is reachable
2. **Unexpected errors** → Check gateway/backend service logs
3. **Test never completes** → Check for deadlocks or infinite waits
4. **Incorrect counts** → Thread safety issue (should not happen with our implementation)

---

## Quick Test Commands

Copy-paste these for quick testing:

```bash
# Test 1: Basic (should all succeed)
curl -sX POST http://localhost:8083/rate-limit-test/execute -H "Content-Type: application/json" -d '{"targetEndpoint":"http://localhost:8081/actuator/health","apiKey":"","totalRequests":10,"concurrentThreads":3,"expectedRateLimit":50}' | jq '{successfulRequests,rateLimitedRequests,testPassed}'

# Test 2: Burst (50 requests, 10 threads)
curl -sX POST http://localhost:8083/rate-limit-test/execute -H "Content-Type: application/json" -d '{"targetEndpoint":"http://localhost:8081/actuator/health","apiKey":"","totalRequests":50,"concurrentThreads":10,"expectedRateLimit":100}' | jq '{successfulRequests,durationMs,requestsPerSecond}'

# Test 3: Controlled rate (with 100ms delay)
curl -sX POST http://localhost:8083/rate-limit-test/execute -H "Content-Type: application/json" -d '{"targetEndpoint":"http://localhost:8081/actuator/health","apiKey":"","totalRequests":20,"concurrentThreads":5,"expectedRateLimit":50,"delayBetweenRequestsMs":100}' | jq '{durationMs,requestsPerSecond}'

# Test 4: Check service info
curl -s http://localhost:8083/rate-limit-test/info | jq '.'
```

---

## Sample Output Explained

```json
{
  "testPassed": true,                    // ✓ Test validation passed
  "totalRequests": 50,                   // Total requests sent
  "successfulRequests": 50,              // Got HTTP 2xx
  "rateLimitedRequests": 0,              // Got HTTP 429
  "errorRequests": 0,                    // Got HTTP 5xx or connection errors
  "unauthorizedRequests": 0,             // Got HTTP 401/403
  "durationMs": 145,                     // Total test duration
  "requestsPerSecond": 344.83,           // Throughput
  "rateLimitViolations": [],             // List of when 429s occurred
  "failureReason": null,                 // Null if passed
  "message": "Rate limit test completed successfully"
}
```

---

## Next Steps

Once you've verified the framework works with these manual tests:

1. **Set up actual rate-limited routes** in your gateway
2. **Configure API keys** with specific rate limits
3. **Test real rate limiting** by exceeding the limits
4. **Verify 429 responses** are returned correctly
5. **Check analytics** to ensure all requests are logged

---

## Troubleshooting

### Issue: "Connection refused"
**Solution:** Check if service is running:
```bash
ps aux | grep load-tester-service
curl http://localhost:8083/actuator/health
```

### Issue: "All requests fail with 404"
**Solution:** Endpoint doesn't exist. Use `/actuator/health` for testing or verify route exists:
```bash
curl http://localhost:8081/actuator/health
```

### Issue: "Test hangs/never completes"
**Solution:** Check logs for exceptions:
```bash
tail -f /tmp/load-tester.log
```

### Issue: "testPassed: false"
**Solution:** Check `failureReason` field in response for details.

---

**Happy Testing! 🚀**
