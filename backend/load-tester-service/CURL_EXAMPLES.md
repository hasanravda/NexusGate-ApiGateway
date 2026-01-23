# Rate Limit Testing - cURL Examples

## Quick Test Commands

### 1. Basic Rate Limit Test
```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/api/users",
    "apiKey": "nx_test_key_12345",
    "totalRequests": 50,
    "concurrentThreads": 10,
    "expectedRateLimit": 20,
    "delayBetweenRequestsMs": 0
  }' | jq '.'
```

### 2. Verify Analytics
```bash
curl -X POST http://localhost:8083/rate-limit-test/verify-analytics \
  -H "Content-Type: application/json" \
  -d '{
    "analyticsBaseUrl": "http://localhost:8085"
  }' | jq '.'
```

### 3. Full Test (Test + Analytics Verification)
```bash
curl -X POST http://localhost:8083/rate-limit-test/full-test \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/api/users",
    "apiKey": "nx_test_key_12345",
    "totalRequests": 50,
    "concurrentThreads": 10,
    "expectedRateLimit": 20,
    "delayBetweenRequestsMs": 0,
    "analyticsBaseUrl": "http://localhost:8085",
    "waitForAnalyticsMs": 2000
  }' | jq '.'
```

### 4. Get Test Service Info
```bash
curl -X GET http://localhost:8083/rate-limit-test/info | jq '.'
```

## Test Scenarios

### Scenario: High Concurrency Burst
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
  }' | jq '{
    testPassed: .testPassed,
    successful: .successfulRequests,
    rateLimited: .rateLimitedRequests,
    throughput: .requestsPerSecond
  }'
```

### Scenario: Controlled Rate Test
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
  }' | jq '.'
```

### Scenario: Test Different Endpoints

#### Test Users Endpoint
```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/api/users",
    "apiKey": "nx_test_key_12345",
    "totalRequests": 30,
    "concurrentThreads": 5,
    "expectedRateLimit": 20
  }' | jq '.successfulRequests'
```

#### Test Orders Endpoint
```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/api/orders",
    "apiKey": "nx_test_key_12345",
    "totalRequests": 30,
    "concurrentThreads": 5,
    "expectedRateLimit": 20
  }' | jq '.successfulRequests'
```

#### Test Payments Endpoint
```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/api/payments",
    "apiKey": "nx_test_key_12345",
    "totalRequests": 20,
    "concurrentThreads": 5,
    "expectedRateLimit": 10
  }' | jq '.successfulRequests'
```

### Scenario: Test Premium API Key (Higher Limits)
```bash
curl -X POST http://localhost:8083/rate-limit-test/execute \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/api/users",
    "apiKey": "nx_lendingkart_prod_abc123",
    "totalRequests": 150,
    "concurrentThreads": 20,
    "expectedRateLimit": 100
  }' | jq '{
    testPassed: .testPassed,
    successful: .successfulRequests,
    rateLimited: .rateLimitedRequests
  }'
```

## Verify Analytics Directly

### Get Analytics Overview
```bash
curl -s http://localhost:8085/analytics/overview | jq '{
  totalRequests: .totalRequests,
  blockedRequests: .blockedRequests,
  errorCount: .errorCount,
  successRate: .successRate
}'
```

### Get Recent Requests
```bash
curl -s http://localhost:8085/analytics/recent-requests?limit=10 | jq '.[] | {
  timestamp: .timestamp,
  endpoint: .endpoint,
  status: .statusCode,
  apiKey: .apiKey
}'
```

### Get Top Endpoints
```bash
curl -s http://localhost:8085/analytics/top-endpoints?limit=5 | jq '.[] | {
  endpoint: .endpoint,
  count: .requestCount
}'
```

## Quick Status Check

### Check All Services Health
```bash
echo "=== Checking Services ==="
echo ""
echo "API Gateway:"
curl -s http://localhost:8081/actuator/health | jq '.status'
echo ""
echo "Load Tester:"
curl -s http://localhost:8083/load-test/health | jq '.status'
echo ""
echo "Analytics:"
curl -s http://localhost:8085/actuator/health | jq '.status'
```

## CI/CD Integration Example

### Run Full Test and Check Exit Code
```bash
#!/bin/bash

# Run full test
RESPONSE=$(curl -s -X POST http://localhost:8083/rate-limit-test/full-test \
  -H "Content-Type: application/json" \
  -d '{
    "targetEndpoint": "http://localhost:8081/api/users",
    "apiKey": "nx_test_key_12345",
    "totalRequests": 50,
    "concurrentThreads": 10,
    "expectedRateLimit": 20,
    "analyticsBaseUrl": "http://localhost:8085",
    "waitForAnalyticsMs": 2000
  }')

# Check overall status
OVERALL_STATUS=$(echo "$RESPONSE" | jq -r '.overallStatus')

if [ "$OVERALL_STATUS" == "PASSED" ]; then
    echo "✓ Rate limit tests PASSED"
    exit 0
else
    echo "✗ Rate limit tests FAILED"
    echo "$RESPONSE" | jq '.'
    exit 1
fi
```

## Expected Responses

### Successful Test Response
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
    "Rate limit exceeded at request #21 (after 20 successful requests)"
  ],
  "message": "Rate limit test completed successfully"
}
```

### Analytics Verification Response
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

### Full Test Response
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
  "message": "✓ Full rate limit test PASSED"
}
```
