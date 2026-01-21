# 🚪 NexusGate API Endpoints Documentation

> **Last Updated**: January 21, 2026  
> **Gateway Port**: 8081

## 📋 Table of Contents
- [Gateway Routes (API Key Required)](#gateway-routes-api-key-required)
  - [User Service](#1-user-service)
  - [Order Service](#2-order-service)
  - [Payment Service](#3-payment-service)
- [Direct Service Routes (No API Key)](#direct-service-routes-no-api-key)
  - [Config Service](#config-service-port-8082)
  - [Auth Service](#auth-service-port-8085)
  - [Load Tester Service](#load-tester-service-port-8086)
  - [Mock Backend Service](#mock-backend-service-port-8091)
- [API Keys](#-available-api-keys)
- [Rate Limits](#-rate-limiting-details)

---

## 🔐 Gateway Routes (API Key Required)

All routes through the gateway require the `X-API-Key` header.

### 1. User Service

**Base URL**: `http://localhost:8081/api/users/**`  
**Backend**: Config Service (Port 8082)  
**🔒 Authentication**: API Key Required  
**Rate Limits**: 
- Default: 200 req/min, 10,000 req/hour
- LendingKart (`nx_lendingkart_prod_abc123`): 1,000 req/min, 60,000 req/hour, 1M req/day

#### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/users` | List all users |
| `GET` | `/api/users/{id}` | Get user by ID |
| `GET` | `/api/users/me?email={email}` | Get current user details |
| `POST` | `/api/users/register` | Register new user |
| `POST` | `/api/users/signin` | User sign in |

**Example Request**:
```bash
curl -X GET http://localhost:8081/api/users \
  -H "X-API-Key: nx_test_key_12345"
```

---

### 2. Order Service

**Base URL**: `http://localhost:8081/api/orders/**`  
**Backend**: Mock Backend Service (Port 8083)  
**🔒 Authentication**: API Key Required  
**Allowed Methods**: GET, POST, PUT, DELETE  
**Rate Limits**: 150 req/min, 8,000 req/hour

#### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/orders` | List all orders |
| `GET` | `/api/orders/{id}` | Get order by ID |
| `GET` | `/api/orders/user/{userId}` | Get orders for specific user |
| `POST` | `/api/orders` | Create new order |
| `DELETE` | `/api/orders/{id}` | Delete order |

**Example Request**:
```bash
curl -X POST http://localhost:8081/api/orders \
  -H "X-API-Key: nx_test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "items": []}'
```

---

### 3. Payment Service

**Base URL**: `http://localhost:8081/api/payments/**`  
**Backend**: Mock Backend Service (Port 8084)  
**🔒 Authentication**: API Key Required  
**Allowed Methods**: POST, GET  
**Rate Limits**: 
- Default: 50 req/min, 2,000 req/hour
- LendingKart (`nx_lendingkart_prod_abc123`): 100 req/min, 6,000 req/hour, 100K req/day

#### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/payments` | List all payments |
| `GET` | `/api/payments/{id}` | Get payment by ID |
| `GET` | `/api/payments/order/{orderId}` | Get payments for specific order |
| `POST` | `/api/payments` | Create/process payment |

**Example Request**:
```bash
curl -X POST http://localhost:8081/api/payments \
  -H "X-API-Key: nx_lendingkart_prod_abc123" \
  -H "Content-Type: application/json" \
  -d '{"orderId": 123, "amount": 1000}'
```

---

## 🔓 Direct Service Routes (No API Key)

These services are accessed directly without going through the gateway.

### Config Service (Port 8082)

Central configuration and management service.

#### Service Routes Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/service-routes` | Get all service routes |
| `GET` | `/service-routes?activeOnly=true` | Get only active routes |
| `GET` | `/service-routes/{id}` | Get route by ID |
| `GET` | `/service-routes/by-path?path={path}` | Get route by path pattern |
| `POST` | `/service-routes` | Create new service route |
| `PUT` | `/service-routes/{id}` | Update service route |
| `PATCH` | `/service-routes/{id}/toggle` | Toggle route active status |
| `DELETE` | `/service-routes/{id}` | Delete service route |

#### API Key Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/keys` | Get all API keys |
| `GET` | `/api/keys/{id}` | Get API key by ID |
| `GET` | `/api/keys/user/{userId}` | Get API keys by creator |
| `GET` | `/api/keys/validate?keyValue={key}` | Validate API key |
| `POST` | `/api/keys` | Create new API key |
| `PUT` | `/api/keys/{id}` | Update API key |
| `DELETE` | `/api/keys/{id}` | Revoke API key |

#### Rate Limit Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/rate-limits` | Get all rate limits |
| `GET` | `/rate-limits/{id}` | Get rate limit by ID |
| `GET` | `/rate-limits/by-api-key/{apiKeyId}` | Get limits for API key |
| `GET` | `/rate-limits/by-service-route/{serviceRouteId}` | Get limits for route |
| `GET` | `/rate-limits/check?apiKeyId={id}&serviceRouteId={id}` | Check effective limit |
| `POST` | `/rate-limits` | Create rate limit |
| `PUT` | `/rate-limits/{id}` | Update rate limit |
| `PATCH` | `/rate-limits/{id}/toggle` | Toggle rate limit status |
| `DELETE` | `/rate-limits/{id}` | Delete rate limit |

**Example Request**:
```bash
curl -X GET http://localhost:8082/service-routes
```

---

### Auth Service (Port 8085)

JWT-based authentication service.

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/auth/login` | User login | ❌ |
| `GET` | `/auth/me` | Get current user info | ✅ JWT |
| `POST` | `/auth/validate` | Validate JWT token | ❌ |
| `POST` | `/auth/refresh` | Refresh JWT token | ✅ JWT |
| `POST` | `/auth/introspect` | Token introspection | ❌ |

**Example Login Request**:
```bash
curl -X POST http://localhost:8085/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@demo.com", "password": "password123"}'
```

**Example with JWT**:
```bash
curl -X GET http://localhost:8085/auth/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Load Tester Service (Port 8086)

Load testing and performance testing service.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/load-test/start` | Start new load test |
| `GET` | `/load-test/status/{testId}` | Get test execution status |
| `GET` | `/load-test/results/{testId}` | Get test results |

**Example Request**:
```bash
curl -X POST http://localhost:8086/load-test/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetKey": "nx_test_key_12345",
    "targetEndpoint": "http://localhost:8081/api/users",
    "requestRate": 100,
    "durationSeconds": 30,
    "concurrencyLevel": 10,
    "requestPattern": "CONSTANT_RATE",
    "httpMethod": "GET"
  }'
```

---

### Mock Backend Service (Port 8091)

Monitoring and metrics endpoints.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/actuator/prometheus` | Prometheus metrics |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/actuator/info` | Application information |

**Example Request**:
```bash
curl -X GET http://localhost:8091/actuator/health
```

---

## 🔑 Available API Keys

Pre-configured API keys in the system:

| Key Name | Key Value | Client | Status |
|----------|-----------|--------|--------|
| LendingKart Production | `nx_lendingkart_prod_abc123` | LendingKart Technologies | ✅ Active |
| PaytmLend Production | `nx_paytm_prod_xyz789` | Paytm Financial Services | ✅ Active |
| MobiKwik Test | `nx_mobikwik_test_def456` | MobiKwik Systems | ✅ Active |
| General Test Key | `nx_test_key_12345` | Test Company | ✅ Active |

### Default User Credentials

For auth service (`/auth/login`):

| Email | Password | Role |
|-------|----------|------|
| `admin@demo.com` | `password123` | ADMIN |
| `manager@demo.com` | `password123` | MANAGER |
| `viewer@demo.com` | `password123` | VIEWER |

---

## 📊 Rate Limiting Details

### Default Rate Limits

| Service | Requests/Minute | Requests/Hour | Requests/Day |
|---------|-----------------|---------------|--------------|
| User Service | 200 | 10,000 | 240,000 |
| Order Service | 150 | 8,000 | 192,000 |
| Payment Service | 50 | 2,000 | 48,000 |

### Premium Rate Limits (LendingKart)

| Service | Requests/Minute | Requests/Hour | Requests/Day |
|---------|-----------------|---------------|--------------|
| User Service | 1,000 | 60,000 | 1,000,000 |
| Payment Service | 100 | 6,000 | 100,000 |

### Premium Rate Limits (PaytmLend)

| Service | Requests/Minute | Requests/Hour | Requests/Day |
|---------|-----------------|---------------|--------------|
| User Service | 500 | 30,000 | 500,000 |

---

## 🚀 Quick Start Guide

### 1. Test Gateway Access (with API Key)

```bash
# List users through gateway
curl -X GET http://localhost:8081/api/users \
  -H "X-API-Key: nx_test_key_12345"
```

### 2. Create New API Key

```bash
# Create new API key via config service
curl -X POST http://localhost:8082/api/keys \
  -H "Content-Type: application/json" \
  -d '{
    "keyName": "My New Key",
    "clientName": "My Company",
    "clientEmail": "contact@mycompany.com",
    "clientCompany": "My Company Ltd",
    "createdByUserId": 1,
    "isActive": true
  }'
```

### 3. Check Rate Limit Status

```bash
# Check effective rate limit for API key + route
curl -X GET "http://localhost:8082/rate-limits/check?apiKeyId=1&serviceRouteId=1"
```

### 4. Login and Get JWT

```bash
# Login to get JWT token
curl -X POST http://localhost:8085/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@demo.com",
    "password": "password123"
  }'
```

---

## 📝 Notes

1. **All gateway routes** (`/api/users/**`, `/api/orders/**`, `/api/payments/**`) require the `X-API-Key` header.
2. **Rate limiting** is enforced per API key per service route combination.
3. **Route configuration** can be managed dynamically via the Config Service without restarting the gateway.
4. **Custom rate limits** can be set for specific API keys on specific routes, overriding defaults.
5. The gateway automatically injects `X-Api-Key-Id` and `X-Route-Id` headers for backend services.

---

## 🔧 Architecture Flow

```
Client Request
    ↓
Gateway (Port 8081) + X-API-Key header
    ↓
[GlobalRequestFilter] → Logs request
    ↓
[AuthenticationFilter] → Validates API key with Config Service
    ↓
[RateLimitFilter] → Checks rate limits (Redis)
    ↓
[ServiceRoutingFilter] → Routes to backend service
    ↓
Backend Service (8082, 8083, 8084)
    ↓
Response back to client
```

---

## 📞 Support

For issues or questions:
- Check gateway logs at port 8081
- Verify API key status via `/api/keys/validate`
- Monitor rate limit usage in Redis
- Review service route configuration via `/service-routes`
