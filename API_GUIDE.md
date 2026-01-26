# 📘 NexusGate API Guide

Complete API reference with examples and best practices.

## 🔑 Authentication

All gateway requests require the `X-API-Key` header.

### Available Test API Keys

| API Key | Rate Limits | Description |
|---------|-------------|-------------|
| `nx_test_key_12345` | 200/min, 10K/hour | Testing key |
| `nx_lendingkart_prod_abc123` | 1000/min, 60K/hour, 1M/day | High-volume production |
| `nx_amazon_prod_xyz789` | 1000/min, 60K/hour | Production key |

## 🌐 Base URLs

| Service | URL | Auth Required |
|---------|-----|---------------|
| Gateway | `http://localhost:8081` | ✅ API Key |
| Config Service | `http://localhost:8082` | ❌ Direct |
| Analytics | `http://localhost:8084` | ❌ Direct |
| Load Tester | `http://localhost:8086` | ❌ Direct |
| Mock Backend | `http://localhost:8091` | ❌ Direct |

## 📋 Gateway API Endpoints

All routes below go through the gateway at `http://localhost:8081`

### 👤 User Service

**Base Path:** `/api/users`

#### Get All Users
```bash
curl -X GET http://localhost:8081/api/users \
  -H "X-API-Key: nx_test_key_12345"
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "USER"
  }
]
```

#### Get User by ID
```bash
curl -X GET http://localhost:8081/api/users/1 \
  -H "X-API-Key: nx_test_key_12345"
```

#### Register New User
```bash
curl -X POST http://localhost:8081/api/users/register \
  -H "X-API-Key: nx_test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "SecurePass123",
    "name": "New User"
  }'
```

#### Sign In
```bash
curl -X POST http://localhost:8081/api/users/signin \
  -H "X-API-Key: nx_test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### 📦 Order Service

**Base Path:** `/api/orders`

#### Get All Orders
```bash
curl -X GET http://localhost:8081/api/orders \
  -H "X-API-Key: nx_test_key_12345"
```

#### Get Order by ID
```bash
curl -X GET http://localhost:8081/api/orders/1 \
  -H "X-API-Key: nx_test_key_12345"
```

#### Create Order
```bash
curl -X POST http://localhost:8081/api/orders \
  -H "X-API-Key: nx_test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "product": "Laptop",
    "quantity": 1,
    "amount": 999.99
  }'
```

### 💳 Payment Service

**Base Path:** `/api/payments`

#### Get All Payments
```bash
curl -X GET http://localhost:8081/api/payments \
  -H "X-API-Key: nx_test_key_12345"
```

#### Process Payment
```bash
curl -X POST http://localhost:8081/api/payments \
  -H "X-API-Key: nx_test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "amount": 999.99,
    "method": "CREDIT_CARD"
  }'
```

## 📊 Analytics API (Direct Access)

**Base URL:** `http://localhost:8084`

### Get All Logs
```bash
curl -X GET "http://localhost:8084/api/logs"
```

**Query Parameters:**
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)
- `apiKey` - Filter by API key
- `method` - Filter by HTTP method
- `startDate` - Start date (ISO format)
- `endDate` - End date (ISO format)

**Example:**
```bash
curl -X GET "http://localhost:8084/api/logs?apiKey=nx_test_key_12345&size=10"
```

### Get Rate Limit Violations
```bash
curl -X GET "http://localhost:8084/api/violations"
```

**Query Parameters:**
- `page`, `size` - Pagination
- `apiKey` - Filter by API key
- `startDate`, `endDate` - Date range

### Get API Metrics
```bash
curl -X GET "http://localhost:8084/api/metrics/summary"
```

**Response:**
```json
{
  "totalRequests": 15420,
  "successfulRequests": 14890,
  "failedRequests": 530,
  "averageResponseTime": 145.5,
  "totalViolations": 23
}
```

## 🧪 Load Testing API

**Base URL:** `http://localhost:8086`

### Run Load Test
```bash
curl -X POST "http://localhost:8086/api/load-test/run" \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "http://localhost:8081/api/users",
    "apiKey": "nx_test_key_12345",
    "requestsPerSecond": 100,
    "durationSeconds": 60,
    "method": "GET"
  }'
```

### Get Test Results
```bash
curl -X GET "http://localhost:8086/api/load-test/results/{testId}"
```

## ⚡ Rate Limiting

### Default Rate Limits
- **Per Minute:** 200 requests
- **Per Hour:** 10,000 requests
- **Per Day:** No limit (for default keys)

### High-Volume Keys
- **Per Minute:** 1,000 requests
- **Per Hour:** 60,000 requests
- **Per Day:** 1,000,000 requests

### Rate Limit Headers
Every response includes:
```
X-RateLimit-Remaining-Minute: 195
X-RateLimit-Remaining-Hour: 9950
X-RateLimit-Limit-Minute: 200
X-RateLimit-Limit-Hour: 10000
```

### 429 Response (Rate Limit Exceeded)
```json
{
  "error": "Rate limit exceeded",
  "message": "Minute limit of 200 requests exceeded",
  "retryAfter": 45
}
```

## 🚨 Error Responses

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Invalid or missing API key",
  "timestamp": "2026-01-26T10:30:00Z"
}
```

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "Route not configured for this path",
  "path": "/api/invalid"
}
```

### 429 Too Many Requests
```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded",
  "retryAfter": 45
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "timestamp": "2026-01-26T10:30:00Z"
}
```

## 🎯 Best Practices

### 1. **Always Include API Key**
```bash
# ✅ Correct
curl -H "X-API-Key: your-key" http://localhost:8081/api/users

# ❌ Wrong (will return 401)
curl http://localhost:8081/api/users
```

### 2. **Handle Rate Limits**
```javascript
// Check rate limit headers
const remaining = response.headers['x-ratelimit-remaining-minute'];
if (remaining < 10) {
  console.warn('Approaching rate limit!');
}

// Handle 429 responses
if (response.status === 429) {
  const retryAfter = response.headers['retry-after'];
  await sleep(retryAfter * 1000);
  // Retry request
}
```

### 3. **Use Pagination**
```bash
# Don't fetch all at once
curl "http://localhost:8084/api/logs?page=0&size=100"
```

### 4. **Set Proper Timeouts**
```javascript
// Configure client with reasonable timeouts
const client = axios.create({
  timeout: 5000, // 5 seconds
  headers: {
    'X-API-Key': 'your-key'
  }
});
```

### 5. **Monitor Your Usage**
- Check analytics dashboard regularly
- Monitor rate limit violations
- Track response times

## 🔧 Testing Tools

### cURL Examples
See [backend/ENDPOINTS.md](backend/ENDPOINTS.md) for more examples.

### Postman Collection
Import the collection from `/postman/NexusGate.postman_collection.json`

### Load Testing
```bash
cd backend/load-tester-service
./test-rate-limiting.sh
```

## 📚 Additional Resources

- **Complete Endpoint List:** [backend/ENDPOINTS.md](backend/ENDPOINTS.md)
- **Backend Documentation:** [backend/PROJECT_DOCUMENTATION.md](backend/PROJECT_DOCUMENTATION.md)
- **Architecture Overview:** [ARCHITECTURE.md](ARCHITECTURE.md)
- **Quick Start:** [QUICKSTART.md](QUICKSTART.md)

---

Need help? Check the main [README.md](README.md) or create an issue!
