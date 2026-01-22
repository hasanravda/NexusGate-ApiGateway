# Quick Test Commands (curl)

## For Windows PowerShell

### 1. Health Check
```powershell
curl http://localhost:8083/load-test/health
```

### 2. Start Basic Load Test
```powershell
$body = @{
    targetKey = "nx_test_key_123"
    targetEndpoint = "http://localhost:8081/api/users"
    requestRate = 50
    durationSeconds = 30
    concurrencyLevel = 5
    requestPattern = "CONSTANT_RATE"
    httpMethod = "GET"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8083/load-test/start" -Method Post -Body $body -ContentType "application/json"
```

### 3. Get Test Status (replace TEST_ID)
```powershell
curl http://localhost:8083/load-test/status/TEST_ID
```

### 4. Get Final Results (replace TEST_ID)
```powershell
curl http://localhost:8083/load-test/result/TEST_ID
```

### 5. Stop Test (replace TEST_ID)
```powershell
Invoke-RestMethod -Uri "http://localhost:8083/load-test/stop/TEST_ID" -Method Delete
```

---

## One-Line Test (PowerShell)
```powershell
Invoke-RestMethod -Uri "http://localhost:8083/load-test/start" -Method Post -Body '{"targetKey":"nx_test_key_123","targetEndpoint":"http://localhost:8081/api/users","requestRate":50,"durationSeconds":30,"concurrencyLevel":5,"requestPattern":"CONSTANT_RATE","httpMethod":"GET"}' -ContentType "application/json"
```

---

## For Git Bash / Linux / Mac

### 1. Health Check
```bash
curl http://localhost:8083/load-test/health
```

### 2. Start Basic Load Test
```bash
curl -X POST http://localhost:8083/load-test/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetKey": "nx_test_key_123",
    "targetEndpoint": "http://localhost:8081/api/users",
    "requestRate": 50,
    "durationSeconds": 30,
    "concurrencyLevel": 5,
    "requestPattern": "CONSTANT_RATE",
    "httpMethod": "GET"
  }'
```

### 3. Start Burst Test
```bash
curl -X POST http://localhost:8083/load-test/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetKey": "nx_test_key_123",
    "targetEndpoint": "http://localhost:8081/api/products",
    "requestRate": 200,
    "durationSeconds": 10,
    "concurrencyLevel": 20,
    "requestPattern": "BURST",
    "httpMethod": "GET"
  }'
```

### 4. Get Test Status (replace TEST_ID)
```bash
curl http://localhost:8083/load-test/status/TEST_ID
```

### 5. Get Final Results (replace TEST_ID)
```bash
curl http://localhost:8083/load-test/result/TEST_ID
```

### 6. Stop Test (replace TEST_ID)
```bash
curl -X DELETE http://localhost:8083/load-test/stop/TEST_ID
```

### 7. List All Tests
```bash
curl http://localhost:8083/load-test/list
```

---

## Complete Test Flow

### Step 1: Start Test
```bash
RESPONSE=$(curl -s -X POST http://localhost:8083/load-test/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetKey": "nx_test_key_123",
    "targetEndpoint": "http://localhost:8081/api/users",
    "requestRate": 50,
    "durationSeconds": 30,
    "concurrencyLevel": 5,
    "requestPattern": "CONSTANT_RATE",
    "httpMethod": "GET"
  }')

echo $RESPONSE
```

### Step 2: Extract Test ID (Linux/Mac)
```bash
TEST_ID=$(echo $RESPONSE | jq -r '.testId')
echo "Test ID: $TEST_ID"
```

### Step 3: Monitor Status
```bash
watch -n 5 "curl -s http://localhost:8083/load-test/status/$TEST_ID | jq ."
```

### Step 4: Get Final Results
```bash
curl -s http://localhost:8083/load-test/result/$TEST_ID | jq .
```

---

## Quick Copy-Paste Tests

### Test 1: Verify Service Works
```bash
curl http://localhost:8083/load-test/health && echo "\nService is UP! ✓"
```

### Test 2: Quick 10-second Load Test
```bash
curl -X POST http://localhost:8083/load-test/start -H "Content-Type: application/json" -d '{"targetKey":"nx_test_key_123","targetEndpoint":"http://localhost:8081/api/users","requestRate":30,"durationSeconds":10,"concurrencyLevel":3,"requestPattern":"CONSTANT_RATE","httpMethod":"GET"}'
```

### Test 3: Stress Test
```bash
curl -X POST http://localhost:8083/load-test/start -H "Content-Type: application/json" -d '{"targetKey":"nx_stress_test","targetEndpoint":"http://localhost:8081/api/users","requestRate":1000,"durationSeconds":30,"concurrencyLevel":100,"requestPattern":"BURST","httpMethod":"GET"}'
```
