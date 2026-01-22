# 🔑 API Key Reference & 401 Error Resolution

## ❌ Problem: 401 Unauthorized Errors

If you're seeing these errors:
```
2026-01-22 19:01:59 - Request failed: 401 Unauthorized from GET http://localhost:8081/api/users
```

**Root Cause:** The API key you're using doesn't exist in the database or is inactive.

---

## ✅ Valid API Keys in Database

These are the **actual valid API keys** that exist in your database:

### 1. Production Keys

#### LendingKart Production Key
```
Key: nx_lendingkart_prod_abc123
Client: LendingKart Technologies
Email: dev@lendingkart.com
Status: Active ✅
Rate Limits:
  - user-service: 1000 req/min, 60000 req/hour
  - payment-service: 100 req/min, 6000 req/hour
```

#### PaytmLend Production Key
```
Key: nx_paytm_prod_xyz789
Client: Paytm Financial Services
Email: api@paytm.com
Status: Active ✅
Rate Limits:
  - user-service: 500 req/min, 30000 req/hour
```

#### MobiKwik Test Key
```
Key: nx_mobikwik_test_def456
Client: MobiKwik Systems
Email: tech@mobikwik.com
Status: Active ✅
```

### 2. Development/Test Key

#### Test Key (Recommended for Testing)
```
Key: nx_test_key_12345
Client: Test Client
Email: test@example.com
Status: Active ✅
```

---

## ⚠️ Common Mistake

### ❌ WRONG (Used in many examples but DOESN'T EXIST):
```json
{
  "targetKey": "nx_test_key_123"
}
```

### ✅ CORRECT (Actually exists in database):
```json
{
  "targetKey": "nx_test_key_12345"
}
```

**Notice:** It's `12345` not `123`

---

## 🚀 Correct Usage Examples

### PowerShell
```powershell
# Use the CORRECT API key
Invoke-RestMethod -Uri "http://localhost:8083/load-test/start" -Method Post -Body '{
  "targetKey": "nx_test_key_12345",
  "targetEndpoint": "http://localhost:8081/api/users",
  "requestRate": 50,
  "durationSeconds": 30,
  "concurrencyLevel": 5,
  "requestPattern": "CONSTANT_RATE",
  "httpMethod": "GET"
}' -ContentType "application/json"
```

### cURL (Bash)
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

### Postman Body
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

---

## 🔍 How to Verify API Keys in Database

### Option 1: Query PostgreSQL
```sql
-- Connect to database
docker exec -it nexusgate-postgres psql -U nexusgate -d nexusgate

-- List all active API keys
SELECT key_value, key_name, client_name, is_active 
FROM api_keys 
WHERE is_active = true;
```

### Option 2: Check init-db.sql
Look at [db/init-db.sql](../db/init-db.sql) file:
```sql
INSERT INTO api_keys (key_value, key_name, ...)
VALUES
    ('nx_lendingkart_prod_abc123', ...),
    ('nx_paytm_prod_xyz789', ...),
    ('nx_mobikwik_test_def456', ...),
    ('nx_test_key_12345', ...);  -- Notice: 12345, not 123
```

---

## 🛠️ Improved Error Handling

The load-tester-service now provides **better error messages**:

### 401 Unauthorized
```
Authentication failed (401 Unauthorized) - API Key may be invalid or inactive.
Endpoint: http://localhost:8081/api/users, Method: GET, API Key: nx_tes***
```

**Solution:**
- Check if the API key exists in database
- Verify the key is active (`is_active = true`)
- Ensure no typos in the key value

### 403 Forbidden
```
Authorization failed (403 Forbidden) - API Key lacks permission for this endpoint.
```

**Solution:**
- Check if the API key has access to the specific route
- Verify rate limits configuration

### 429 Too Many Requests
```
Rate limit exceeded (429 Too Many Requests)
```

**Solution:**
- This is **expected behavior** during load testing
- It means rate limiting is working correctly
- Adjust your test parameters or rate limits

### 500 Server Error
```
Server error (500) - Endpoint: http://localhost:8081/api/users
```

**Solution:**
- Check gateway logs: `docker logs nexusgate-gateway`
- Check backend service logs
- Verify services are running

### Connection Errors
```
Connection error - Endpoint: http://localhost:8081/api/users, Error: Connection refused
```

**Solution:**
- Verify gateway is running on port 8081
- Check: `curl http://localhost:8081/actuator/health`
- Restart services: `docker compose up --build`

---

## 📊 Test Scenarios with Valid Keys

### Test 1: Basic Test (Low Volume)
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
**Expected:** Mix of 200 and 429 responses

### Test 2: High Volume (LendingKart Key)
```json
{
  "targetKey": "nx_lendingkart_prod_abc123",
  "targetEndpoint": "http://localhost:8081/api/users",
  "requestRate": 500,
  "durationSeconds": 60,
  "concurrencyLevel": 50,
  "requestPattern": "CONSTANT_RATE",
  "httpMethod": "GET"
}
```
**Expected:** Higher throughput due to higher rate limits (1000 req/min)

### Test 3: Different Endpoints
```json
{
  "targetKey": "nx_lendingkart_prod_abc123",
  "targetEndpoint": "http://localhost:8081/api/orders",
  "requestRate": 100,
  "durationSeconds": 30,
  "concurrencyLevel": 10,
  "requestPattern": "CONSTANT_RATE",
  "httpMethod": "GET"
}
```

---

## 🔧 Adding New API Keys

### Step 1: Add to Database
```sql
INSERT INTO api_keys (
    key_value, key_name, client_name, client_email,
    client_company, created_by_user_id, is_active
)
VALUES (
    'nx_your_custom_key',
    'Your Custom Key',
    'Your Client',
    'client@example.com',
    'Your Company',
    1,
    true
);
```

### Step 2: Configure Rate Limits (Optional)
```sql
INSERT INTO rate_limits (api_key_id, service_route_id, requests_per_minute, requests_per_hour, is_active)
SELECT
    ak.id,
    sr.id,
    100,  -- Custom rate limit per minute
    6000, -- Custom rate limit per hour
    true
FROM api_keys ak
CROSS JOIN service_routes sr
WHERE ak.key_value = 'nx_your_custom_key'
AND sr.service_name = 'user-service';
```

### Step 3: Test the New Key
```bash
curl -X POST http://localhost:8083/load-test/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetKey": "nx_your_custom_key",
    "targetEndpoint": "http://localhost:8081/api/users",
    "requestRate": 50,
    "durationSeconds": 30,
    "concurrencyLevel": 5,
    "requestPattern": "CONSTANT_RATE",
    "httpMethod": "GET"
  }'
```

---

## 📝 Summary

### ✅ DO:
- Use `nx_test_key_12345` for testing
- Use `nx_lendingkart_prod_abc123` for high-volume tests
- Check database for available keys
- Monitor logs for detailed error messages

### ❌ DON'T:
- Use `nx_test_key_123` (doesn't exist)
- Forget to check `is_active` status
- Ignore 401 errors (they mean authentication failure)

---

## 🎯 Quick Fix Command

If you're getting 401 errors, try this immediately:

```powershell
# Test with the CORRECT API key
Invoke-RestMethod -Uri "http://localhost:8083/load-test/start" -Method Post -Body '{
  "targetKey": "nx_test_key_12345",
  "targetEndpoint": "http://localhost:8081/api/users",
  "requestRate": 10,
  "durationSeconds": 10,
  "concurrencyLevel": 2,
  "requestPattern": "CONSTANT_RATE",
  "httpMethod": "GET"
}' -ContentType "application/json"
```

**This should work!** 🎉

---

## 📞 Still Having Issues?

1. **Check gateway logs:**
   ```bash
   docker logs nexusgate-gateway
   ```

2. **Verify database:**
   ```bash
   docker exec -it nexusgate-postgres psql -U nexusgate -d nexusgate -c "SELECT * FROM api_keys WHERE is_active = true;"
   ```

3. **Check service health:**
   ```bash
   curl http://localhost:8081/actuator/health
   curl http://localhost:8083/load-test/health
   ```

4. **Restart services:**
   ```bash
   cd backend
   docker compose down -v
   docker compose up --build
   ```

---

**Updated:** January 22, 2026  
**Status:** All endpoints working with correct API keys ✅
