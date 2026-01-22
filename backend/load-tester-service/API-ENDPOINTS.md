# Load Tester Service - API Endpoints Reference

## ✅ Fixed Issue
The `/load-test/list` and `/load-test/result/{testId}` endpoints were missing. They have been added to the controller.

---

## 📡 Available Endpoints

### Base URL
```
http://localhost:8083/load-test
```

---

## 1. Health Check ✅
**Endpoint:** `GET /load-test/health`  
**Description:** Check if the service is running  
**Response:**
```json
{
  "status": "UP",
  "service": "load-tester-service"
}
```

**cURL:**
```bash
curl http://localhost:8083/load-test/health
```

**PowerShell:**
```powershell
curl http://localhost:8083/load-test/health
```

---

## 2. Start Load Test ✅
**Endpoint:** `POST /load-test/start`  
**Description:** Start a new load test  
**Request Body:**
```json
{
  "targetKey": "nx_test_key_12345",
  "targetEndpoint": "http://localhost:8081/api/users",
  "requestRate": 50,
  "durationSeconds": 30,
  "concurrencyLevel": 5,
  "requestPattern": "CONSTANT_RATE",
  "httpMethod": "GET"
}
```

**Response:**
```json
{
  "testId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "message": "Load test started successfully"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8083/load-test/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetKey": "nx_test_key_12345",
    "targetEndpoint": "http://localhost:8081/api/users",
    "requestRate": 50,
    "durationSeconds": 30,
    "concurrencyLevel": 5,
    "requestPattern": "CONSTANT_RATE",
    "httpMethod": "GET"
  }'
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8083/load-test/start" -Method Post -Body '{"targetKey":"nx_test_key_123","targetEndpoint":"http://localhost:8081/api/users","requestRate":50,"durationSeconds":30,"concurrencyLevel":5,"requestPattern":"CONSTANT_RATE","httpMethod":"GET"}' -ContentType "application/json"
```

---

## 3. Get Test Status (Real-time) ✅
**Endpoint:** `GET /load-test/status/{testId}`  
**Description:** Get current status and metrics of a running or completed test  
**Response:**
```json
{
  "testId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "totalRequests": 1500,
  "successfulRequests": 1200,
  "rateLimitedRequests": 300,
  "errorRequests": 0,
  "averageLatencyMs": 45.3,
  "p95LatencyMs": 120,
  "minLatencyMs": 20,
  "maxLatencyMs": 350,
  "requestsPerSecond": 50.0,
  "successRate": 80.0,
  "rateLimitRate": 20.0,
  "targetEndpoint": "http://localhost:8081/api/users",
  "configuredRequestRate": 50,
  "concurrencyLevel": 5
}
```

**cURL:**
```bash
curl http://localhost:8083/load-test/status/YOUR_TEST_ID
```

**PowerShell:**
```powershell
curl http://localhost:8083/load-test/status/YOUR_TEST_ID
```

---

## 4. Get Test Result (Final) ✅ **[NEWLY ADDED]**
**Endpoint:** `GET /load-test/result/{testId}`  
**Description:** Get comprehensive final results of a test  
**Response:**
```json
{
  "testId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "totalRequests": 1500,
  "successfulRequests": 1200,
  "rateLimitedRequests": 300,
  "errorRequests": 0,
  "averageLatencyMs": 45.3,
  "p95LatencyMs": 120,
  "minLatencyMs": 20,
  "maxLatencyMs": 350,
  "requestsPerSecond": 50.0,
  "successRate": 80.0,
  "rateLimitRate": 20.0,
  "errorRate": 0.0,
  "testDurationSeconds": 30,
  "targetEndpoint": "http://localhost:8081/api/users",
  "targetKey": "nx_test_key_123",
  "configuredRequestRate": 50,
  "concurrencyLevel": 5,
  "requestPattern": "CONSTANT_RATE",
  "httpMethod": "GET",
  "statusCodeDistribution": {
    "200": 1200,
    "429": 300
  },
  "startTime": "2026-01-22T13:00:00",
  "endTime": "2026-01-22T13:00:30"
}
```

**cURL:**
```bash
curl http://localhost:8083/load-test/result/YOUR_TEST_ID
```

**PowerShell:**
```powershell
curl http://localhost:8083/load-test/result/YOUR_TEST_ID
```

---

## 5. Get Test Results (Alternative Path) ✅
**Endpoint:** `GET /load-test/results/{testId}`  
**Description:** Same as `/load-test/result/{testId}` (alternative endpoint)  

**cURL:**
```bash
curl http://localhost:8083/load-test/results/YOUR_TEST_ID
```

---

## 6. List All Tests ✅ **[NEWLY ADDED]**
**Endpoint:** `GET /load-test/list`  
**Description:** Get all test executions (running and completed)  
**Response:**
```json
[
  {
    "testId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "RUNNING",
    "targetEndpoint": "http://localhost:8081/api/users",
    "requestRate": 50,
    "totalRequests": 1500,
    "successRate": 80.0,
    "startTime": "2026-01-22T13:00:00",
    "endTime": null
  },
  {
    "testId": "661f9511-f3ac-52e5-b827-557766551111",
    "status": "COMPLETED",
    "targetEndpoint": "http://localhost:8081/api/products",
    "requestRate": 100,
    "totalRequests": 3000,
    "successRate": 95.0,
    "startTime": "2026-01-22T12:00:00",
    "endTime": "2026-01-22T12:00:30"
  }
]
```

**cURL:**
```bash
curl http://localhost:8083/load-test/list
```

**PowerShell:**
```powershell
curl http://localhost:8083/load-test/list
```

**Postman:**
- Method: GET
- URL: `http://localhost:8083/load-test/list`

---

## 7. Stop Running Test ✅
**Endpoint:** `DELETE /load-test/stop/{testId}`  
**Description:** Stop a running test gracefully  
**Response:**
```json
{
  "testId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Test stop requested"
}
```

**cURL:**
```bash
curl -X DELETE http://localhost:8083/load-test/stop/YOUR_TEST_ID
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8083/load-test/stop/YOUR_TEST_ID" -Method Delete
```

---

## 🔧 Error Responses

### Test Not Found (404)
```json
{
  "timestamp": "2026-01-22T13:24:31.463Z",
  "status": 404,
  "error": "Not Found",
  "path": "/load-test/status/invalid-id"
}
```

### Invalid Request (400)
```json
{
  "error": "Cannot stop test",
  "message": "Test is not running"
}
```

### Internal Server Error (500)
```json
{
  "error": "Failed to start load test",
  "message": "Connection refused"
}
```

---

## 📊 Complete Test Workflow

### 1. Check Service Health
```bash
curl http://localhost:8083/load-test/health
```

### 2. Start a Test
```bash
curl -X POST http://localhost:8083/load-test/start \
  -H "Content-Type: application/json" \
  -d '{"targetKey":"nx_test_key_123","targetEndpoint":"http://localhost:8081/api/users","requestRate":50,"durationSeconds":30,"concurrencyLevel":5,"requestPattern":"CONSTANT_RATE","httpMethod":"GET"}'
```

**Copy the `testId` from the response!**

### 3. Monitor Status (Repeat every few seconds)
```bash
curl http://localhost:8083/load-test/status/YOUR_TEST_ID
```

### 4. Get Final Results (After completion)
```bash
curl http://localhost:8083/load-test/result/YOUR_TEST_ID
```

### 5. List All Tests
```bash
curl http://localhost:8083/load-test/list
```

---

## 🎯 Quick Tests

### Quick Test 1: Verify All Endpoints Work
```powershell
# Health check
curl http://localhost:8083/load-test/health

# List all tests
curl http://localhost:8083/load-test/list

# Start a test
$response = Invoke-RestMethod -Uri "http://localhost:8083/load-test/start" -Method Post -Body '{"targetKey":"nx_test_key_12345","targetEndpoint":"http://localhost:8081/api/users","requestRate":50,"durationSeconds":30,"concurrencyLevel":5,"requestPattern":"CONSTANT_RATE","httpMethod":"GET"}' -ContentType "application/json"

# Get the test ID
$testId = $response.testId
Write-Host "Test ID: $testId"

# Check status
curl "http://localhost:8083/load-test/status/$testId"

# Wait 10 seconds
Start-Sleep -Seconds 10

# Check status again
curl "http://localhost:8083/load-test/status/$testId"

# Get final result (wait for completion or check)
curl "http://localhost:8083/load-test/result/$testId"

# List all tests again
curl http://localhost:8083/load-test/list
```

---

## 📝 Changes Made

### Added Endpoints:
1. ✅ `GET /load-test/list` - List all tests
2. ✅ `GET /load-test/result/{testId}` - Get final results (single form)

### Modified Files:
1. **LoadTestController.java**
   - Added `listAllTests()` method for `/list` endpoint
   - Added `getTestResult()` method for `/result/{testId}` endpoint
   - Added import for `List` and `Collectors`

2. **LoadTestService.java**
   - Added `getAllTests()` method to retrieve all test executions

### Services Restarted:
- Docker Compose services rebuilt to pick up changes

---

## ✅ All Endpoints Now Working!

The 404 error for `/load-test/list` has been fixed. All endpoints are now available and functional! 🎉
