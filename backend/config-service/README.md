# Config Service - API Management & Configuration

**Port:** 8082

## Overview

The Config Service is the **central configuration and API management hub** for NexusGate. It provides RESTful APIs for managing users, API keys, service routes, and rate limits - all stored in PostgreSQL for ACID compliance.

### Key Responsibilities

✅ **User Management** - Admin users with role-based access
✅ **API Key Management** - Generate, validate, and revoke API keys
✅ **Route Configuration** - Dynamic service route management
✅ **Rate Limit Policies** - Per-client, per-route rate limit configuration
✅ **Database Operations** - Transaction management with Spring Data JPA

### Key Features

- **RESTful APIs** - Standard HTTP endpoints for all operations
- **ACID Transactions** - PostgreSQL ensures data consistency
- **Connection Pooling** - HikariCP for efficient database access
- **Validation** - Input validation with Spring Validation
- **Error Handling** - Standardized error responses

### Performance

| Metric | Value | Notes |
|--------|-------|-------|
| **API Response Time** | 10-50ms | With database queries |
| **Connection Pool** | 10 connections | Configurable via HikariCP |
| **Throughput** | 500+ req/sec | Blocking I/O (Spring MVC) |

---

## Base URL
```
http://localhost:8082
```

---

## 📋 Table of Contents
- [User Management API](#-user-management-api)
- [API Key Management](#-api-key-management)
- [Service Route Management](#-service-route-management)
- [Rate Limit Management](#-rate-limit-management)

---

## 👤 User Management API
Base Path: `/api/users`

### 1. Register New User
**POST** `/api/users/register`

**Request Body:**
```json
{
  "email": "admin@nexusgate.com",
  "password": "secure123",
  "fullName": "John Doe",
  "role": "ADMIN"
}
```

**Response:** `201 CREATED`
```json
{
  "id": 1,
  "email": "admin@nexusgate.com",
  "fullName": "John Doe",
  "role": "ADMIN",
  "createdAt": "2026-01-19T10:30:00"
}
```

---

### 2. Sign In
**POST** `/api/users/signin`

**Request Body:**
```json
{
  "email": "admin@nexusgate.com",
  "password": "secure123"
}
```

**Response:** `200 OK`
```json
{
  "userId": 1,
  "email": "admin@nexusgate.com",
  "fullName": "John Doe",
  "role": "ADMIN",
  "message": "Sign in successful"
}
```

---

### 3. Get All Users
**GET** `/api/users`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "email": "admin@nexusgate.com",
    "fullName": "John Doe",
    "role": "ADMIN",
    "createdAt": "2026-01-19T10:30:00"
  }
]
```

---

### 4. Get User by ID
**GET** `/api/users/{id}`

**Example:** `GET /api/users/1`

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "admin@nexusgate.com",
  "fullName": "John Doe",
  "role": "ADMIN",
  "createdAt": "2026-01-19T10:30:00"
}
```

---

### 5. Get Current User
**GET** `/api/users/me?email={email}`

**Example:** `GET /api/users/me?email=admin@nexusgate.com`

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "admin@nexusgate.com",
  "fullName": "John Doe",
  "role": "ADMIN",
  "createdAt": "2026-01-19T10:30:00"
}
```

---

## 🔑 API Key Management
Base Path: `/api/keys`

### 1. Create API Key
**POST** `/api/keys`

**Request Body:**
```json
{
  "keyName": "Production Key - Acme Corp",
  "clientName": "Acme Corporation",
  "clientEmail": "api@acme.com",
  "clientCompany": "Acme Technologies Ltd",
  "createdByUserId": 1,
  "expiresAt": "2027-12-31T23:59:59",
  "notes": "Main production API key"
}
```

**Response:** `201 CREATED`
```json
{
  "id": 6,
  "keyValue": "nx_abc123xyz789def456ghi",
  "keyName": "Production Key - Acme Corp",
  "clientName": "Acme Corporation",
  "clientEmail": "api@acme.com",
  "clientCompany": "Acme Technologies Ltd",
  "createdByUserId": 1,
  "isActive": true,
  "expiresAt": "2027-12-31T23:59:59",
  "lastUsedAt": null,
  "createdAt": "2026-01-19T10:30:00",
  "notes": "Main production API key"
}
```

---

### 2. Get All API Keys ✅ (Tested)
**GET** `/api/keys`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "keyValue": "nx_lendingkart_prod_abc123",
    "keyName": "LendingKart Production Key",
    "clientName": "LendingKart",
    "clientEmail": "dev@lendingkart.com",
    "clientCompany": "LendingKart Technologies",
    "createdByUserId": 1,
    "isActive": true,
    "expiresAt": null,
    "lastUsedAt": null,
    "createdAt": "2026-01-12T15:50:55.26787",
    "notes": null
  }
]
```

---

### 3. Get API Key by ID ✅ (Tested)
**GET** `/api/keys/{id}`

**Example:** `GET /api/keys/1`

**Response:** `200 OK`
```json
{
  "id": 1,
  "keyValue": "nx_lendingkart_prod_abc123",
  "keyName": "LendingKart Production Key",
  "clientName": "LendingKart",
  "clientEmail": "dev@lendingkart.com",
  "clientCompany": "LendingKart Technologies",
  "createdByUserId": 1,
  "isActive": true,
  "expiresAt": null,
  "lastUsedAt": null,
  "createdAt": "2026-01-12T15:50:55.26787",
  "notes": null
}
```

---

### 4. Get API Keys by User ID ✅ (Tested)
**GET** `/api/keys/user/{userId}`

**Example:** `GET /api/keys/user/1`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "keyValue": "nx_lendingkart_prod_abc123",
    "keyName": "LendingKart Production Key",
    "clientName": "LendingKart",
    "clientEmail": "dev@lendingkart.com",
    "clientCompany": "LendingKart Technologies",
    "createdByUserId": 1,
    "isActive": true,
    "expiresAt": null,
    "lastUsedAt": null,
    "createdAt": "2026-01-12T15:50:55.26787",
    "notes": null
  }
]
```

---

### 5. Validate API Key
**GET** `/api/keys/validate?keyValue={keyValue}`

**Example:** `GET /api/keys/validate?keyValue=nx_lendingkart_prod_abc123`

**Response:** `200 OK`
```json
{
  "id": 1,
  "keyValue": "nx_lendingkart_prod_abc123",
  "keyName": "LendingKart Production Key",
  "clientName": "LendingKart",
  "clientEmail": "dev@lendingkart.com",
  "clientCompany": "LendingKart Technologies",
  "createdByUserId": 1,
  "isActive": true,
  "expiresAt": null,
  "lastUsedAt": "2026-01-19T10:30:00",
  "createdAt": "2026-01-12T15:50:55.26787",
  "notes": null
}
```

---

### 6. Update API Key
**PUT** `/api/keys/{id}`

**Example:** `PUT /api/keys/1`

**Request Body:**
```json
{
  "keyName": "LendingKart Production Key - Updated",
  "clientName": "LendingKart",
  "clientEmail": "dev@lendingkart.com",
  "clientCompany": "LendingKart Technologies Pvt Ltd",
  "createdByUserId": 1,
  "expiresAt": "2027-12-31T23:59:59",
  "notes": "Updated with expiration date"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "keyValue": "nx_lendingkart_prod_abc123",
  "keyName": "LendingKart Production Key - Updated",
  "clientName": "LendingKart",
  "clientEmail": "dev@lendingkart.com",
  "clientCompany": "LendingKart Technologies Pvt Ltd",
  "createdByUserId": 1,
  "isActive": true,
  "expiresAt": "2027-12-31T23:59:59",
  "lastUsedAt": null,
  "createdAt": "2026-01-12T15:50:55.26787",
  "notes": "Updated with expiration date"
}
```

---

### 7. Revoke API Key
**DELETE** `/api/keys/{id}`

**Example:** `DELETE /api/keys/3`

**Response:** `204 NO CONTENT`

---

## 🛣️ Service Route Management
Base Path: `/service-routes`

### 1. Create Service Route ✅ (Tested)
**POST** `/service-routes`

**Request Body:**
```json
{
  "serviceName": "notification-service",
  "serviceDescription": "Handles email and SMS notifications",
  "publicPath": "/api/notifications/**",
  "targetUrl": "http://localhost:8086/notifications",
  "allowedMethods": ["GET", "POST"],
  "rateLimitPerMinute": 100,
  "rateLimitPerHour": 5000,
  "createdByUserId": 1,
  "notes": "Internal notification service"
}
```

**Response:** `201 CREATED`
```json
{
  "id": 5,
  "serviceName": "notification-service",
  "serviceDescription": "Handles email and SMS notifications",
  "publicPath": "/api/notifications/**",
  "targetUrl": "http://localhost:8086/notifications",
  "allowedMethods": ["GET", "POST"],
  "rateLimitPerMinute": 100,
  "rateLimitPerHour": 5000,
  "isActive": true,
  "createdByUserId": 1,
  "createdAt": "2026-01-19T10:30:00",
  "updatedAt": "2026-01-19T10:30:00",
  "notes": "Internal notification service"
}
```

---

### 2. Get All Service Routes ✅ (Tested)
**GET** `/service-routes`

**Query Parameters:**
- `activeOnly` (optional, default: false) - Set to `true` to get only active routes

**Example 1:** `GET /service-routes`

**Example 2:** `GET /service-routes?activeOnly=true`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "serviceName": "user-service",
    "serviceDescription": "Handles all user management operations",
    "publicPath": "/api/users/**",
    "targetUrl": "http://localhost:8082/users",
    "allowedMethods": ["GET", "POST", "PUT", "DELETE"],
    "rateLimitPerMinute": 200,
    "rateLimitPerHour": 10000,
    "isActive": true,
    "createdByUserId": 1,
    "createdAt": "2026-01-12T15:50:55.197312",
    "updatedAt": "2026-01-12T15:50:55.197312",
    "notes": null
  }
]
```

---

### 3. Get Service Route by ID
**GET** `/service-routes/{id}`

**Example:** `GET /service-routes/1`

**Response:** `200 OK`
```json
{
  "id": 1,
  "serviceName": "user-service",
  "serviceDescription": "Handles all user management operations",
  "publicPath": "/api/users/**",
  "targetUrl": "http://localhost:8082/users",
  "allowedMethods": ["GET", "POST", "PUT", "DELETE"],
  "rateLimitPerMinute": 200,
  "rateLimitPerHour": 10000,
  "isActive": true,
  "createdByUserId": 1,
  "createdAt": "2026-01-12T15:50:55.197312",
  "updatedAt": "2026-01-12T15:50:55.197312",
  "notes": null
}
```

---

### 4. Get Service Route by Path
**GET** `/service-routes/by-path?path={path}`

**Example:** `GET /service-routes/by-path?path=/api/users/**`

**Response:** `200 OK`
```json
{
  "id": 1,
  "serviceName": "user-service",
  "serviceDescription": "Handles all user management operations",
  "publicPath": "/api/users/**",
  "targetUrl": "http://localhost:8082/users",
  "allowedMethods": ["GET", "POST", "PUT", "DELETE"],
  "rateLimitPerMinute": 200,
  "rateLimitPerHour": 10000,
  "isActive": true,
  "createdByUserId": 1,
  "createdAt": "2026-01-12T15:50:55.197312",
  "updatedAt": "2026-01-12T15:50:55.197312",
  "notes": null
}
```

---

### 5. Update Service Route
**PUT** `/service-routes/{id}`

**Example:** `PUT /service-routes/1`

**Request Body:**
```json
{
  "serviceName": "user-service",
  "serviceDescription": "Handles all user management operations - Updated",
  "publicPath": "/api/users/**",
  "targetUrl": "http://localhost:8082/users",
  "allowedMethods": ["GET", "POST", "PUT", "DELETE", "PATCH"],
  "rateLimitPerMinute": 300,
  "rateLimitPerHour": 15000,
  "createdByUserId": 1,
  "notes": "Increased rate limits for production"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "serviceName": "user-service",
  "serviceDescription": "Handles all user management operations - Updated",
  "publicPath": "/api/users/**",
  "targetUrl": "http://localhost:8082/users",
  "allowedMethods": ["GET", "POST", "PUT", "DELETE", "PATCH"],
  "rateLimitPerMinute": 300,
  "rateLimitPerHour": 15000,
  "isActive": true,
  "createdByUserId": 1,
  "createdAt": "2026-01-12T15:50:55.197312",
  "updatedAt": "2026-01-19T10:30:00",
  "notes": "Increased rate limits for production"
}
```

---

### 6. Toggle Service Route Active Status
**PATCH** `/service-routes/{id}/toggle`

**Example:** `PATCH /service-routes/1/toggle`

**Response:** `200 OK`
```json
{
  "id": 1,
  "serviceName": "user-service",
  "serviceDescription": "Handles all user management operations",
  "publicPath": "/api/users/**",
  "targetUrl": "http://localhost:8082/users",
  "allowedMethods": ["GET", "POST", "PUT", "DELETE"],
  "rateLimitPerMinute": 200,
  "rateLimitPerHour": 10000,
  "isActive": false,
  "createdByUserId": 1,
  "createdAt": "2026-01-12T15:50:55.197312",
  "updatedAt": "2026-01-19T10:30:00",
  "notes": null
}
```

---

### 7. Delete Service Route
**DELETE** `/service-routes/{id}`

**Example:** `DELETE /service-routes/5`

**Response:** `204 NO CONTENT`

---

## ⏱️ Rate Limit Management
Base Path: `/rate-limits`

### 1. Create Rate Limit
**POST** `/rate-limits`

**Request Body Examples:**

**Specific Rate Limit (API Key + Service Route):**
```json
{
  "apiKeyId": 1,
  "serviceRouteId": 2,
  "requestsPerMinute": 50,
  "requestsPerHour": 2000,
  "requestsPerDay": 40000,
  "notes": "Custom limit for LendingKart on order-service"
}
```

**Default Route Rate Limit (applies to all API keys for this route):**
```json
{
  "apiKeyId": null,
  "serviceRouteId": 2,
  "requestsPerMinute": 100,
  "requestsPerHour": 5000,
  "requestsPerDay": 100000,
  "notes": "Default limit for order-service"
}
```

**Global API Key Rate Limit (applies to all routes for this key):**
```json
{
  "apiKeyId": 1,
  "serviceRouteId": null,
  "requestsPerMinute": 500,
  "requestsPerHour": 20000,
  "requestsPerDay": 400000,
  "notes": "Global limit for LendingKart across all services"
}
```

**Response:** `201 CREATED`
```json
{
  "id": 1,
  "apiKeyId": 1,
  "serviceRouteId": 2,
  "requestsPerMinute": 50,
  "requestsPerHour": 2000,
  "requestsPerDay": 40000,
  "isActive": true,
  "createdAt": "2026-01-19T10:30:00",
  "updatedAt": "2026-01-19T10:30:00",
  "notes": "Custom limit for LendingKart on order-service"
}
```

---

### 2. Get All Rate Limits
**GET** `/rate-limits`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "apiKeyId": 1,
    "serviceRouteId": 2,
    "requestsPerMinute": 50,
    "requestsPerHour": 2000,
    "requestsPerDay": 40000,
    "isActive": true,
    "createdAt": "2026-01-19T10:30:00",
    "updatedAt": "2026-01-19T10:30:00",
    "notes": "Custom limit for LendingKart on order-service"
  }
]
```

---

### 3. Get Rate Limit by ID
**GET** `/rate-limits/{id}`

**Example:** `GET /rate-limits/1`

**Response:** `200 OK`
```json
{
  "id": 1,
  "apiKeyId": 1,
  "serviceRouteId": 2,
  "requestsPerMinute": 50,
  "requestsPerHour": 2000,
  "requestsPerDay": 40000,
  "isActive": true,
  "createdAt": "2026-01-19T10:30:00",
  "updatedAt": "2026-01-19T10:30:00",
  "notes": "Custom limit for LendingKart on order-service"
}
```

---

### 4. Get Rate Limits by API Key
**GET** `/rate-limits/by-api-key/{apiKeyId}`

**Example:** `GET /rate-limits/by-api-key/1`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "apiKeyId": 1,
    "serviceRouteId": 2,
    "requestsPerMinute": 50,
    "requestsPerHour": 2000,
    "requestsPerDay": 40000,
    "isActive": true,
    "createdAt": "2026-01-19T10:30:00",
    "updatedAt": "2026-01-19T10:30:00",
    "notes": "Custom limit for LendingKart on order-service"
  }
]
```

---

### 5. Get Rate Limits by Service Route
**GET** `/rate-limits/by-service-route/{serviceRouteId}`

**Example:** `GET /rate-limits/by-service-route/2`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "apiKeyId": 1,
    "serviceRouteId": 2,
    "requestsPerMinute": 50,
    "requestsPerHour": 2000,
    "requestsPerDay": 40000,
    "isActive": true,
    "createdAt": "2026-01-19T10:30:00",
    "updatedAt": "2026-01-19T10:30:00",
    "notes": "Custom limit for LendingKart on order-service"
  }
]
```

---

### 6. Check Effective Rate Limit 🔥 CRITICAL
**GET** `/rate-limits/check?apiKeyId={apiKeyId}&serviceRouteId={serviceRouteId}`

**Description:** This is the most important endpoint for the gateway! It returns the effective rate limit for a specific API key + service route combination.

**Priority Order:**
1. **Specific:** apiKeyId + serviceRouteId (most specific)
2. **Route Default:** serviceRouteId only (applies to all keys)
3. **Key Global:** apiKeyId only (applies to all routes)
4. **System Default:** 1000 req/min (fallback)

**Example:** `GET /rate-limits/check?apiKeyId=1&serviceRouteId=2`

**Response:** `200 OK`
```json
{
  "requestsPerMinute": 50,
  "requestsPerHour": 2000,
  "requestsPerDay": 40000,
  "source": "SPECIFIC",
  "rateLimitId": 1,
  "apiKeyId": 1,
  "serviceRouteId": 2,
  "notes": "Custom limit for LendingKart on order-service"
}
```

**Possible `source` values:**
- `SPECIFIC` - Specific rate limit for this API key + route
- `ROUTE_DEFAULT` - Default rate limit for the route
- `KEY_GLOBAL` - Global rate limit for the API key
- `SYSTEM_DEFAULT` - System fallback (1000/min)

---

### 7. Update Rate Limit
**PUT** `/rate-limits/{id}`

**Example:** `PUT /rate-limits/1`

**Request Body:**
```json
{
  "apiKeyId": 1,
  "serviceRouteId": 2,
  "requestsPerMinute": 100,
  "requestsPerHour": 4000,
  "requestsPerDay": 80000,
  "notes": "Increased limit for LendingKart on order-service"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "apiKeyId": 1,
  "serviceRouteId": 2,
  "requestsPerMinute": 100,
  "requestsPerHour": 4000,
  "requestsPerDay": 80000,
  "isActive": true,
  "createdAt": "2026-01-19T10:30:00",
  "updatedAt": "2026-01-19T11:00:00",
  "notes": "Increased limit for LendingKart on order-service"
}
```

---

### 8. Toggle Rate Limit Active Status
**PATCH** `/rate-limits/{id}/toggle`

**Example:** `PATCH /rate-limits/1/toggle`

**Response:** `200 OK`
```json
{
  "id": 1,
  "apiKeyId": 1,
  "serviceRouteId": 2,
  "requestsPerMinute": 50,
  "requestsPerHour": 2000,
  "requestsPerDay": 40000,
  "isActive": false,
  "createdAt": "2026-01-19T10:30:00",
  "updatedAt": "2026-01-19T11:00:00",
  "notes": "Custom limit for LendingKart on order-service"
}
```

---

### 9. Delete Rate Limit
**DELETE** `/rate-limits/{id}`

**Example:** `DELETE /rate-limits/1`

**Response:** `204 NO CONTENT`

---

## 📊 Testing Summary

### ✅ Already Tested (from image)
1. **GET** `/api/keys` - Get all API keys
2. **GET** `/api/keys/{id}` - Get API key by ID
3. **GET** `/api/keys/user/{userId}` - Get API keys by user
4. **GET** `/service-routes` - Get all routes

### 🧪 Remaining Tests

#### User Management (5 endpoints)
- [ ] POST `/api/users/register`
- [ ] POST `/api/users/signin`
- [ ] GET `/api/users`
- [ ] GET `/api/users/{id}`
- [ ] GET `/api/users/me?email=`

#### API Key Management (4 endpoints)
- [ ] POST `/api/keys`
- [ ] GET `/api/keys/validate?keyValue=`
- [ ] PUT `/api/keys/{id}`
- [ ] DELETE `/api/keys/{id}`

#### Service Route Management (6 endpoints)
- [ ] POST `/service-routes`
- [ ] GET `/service-routes/{id}`
- [ ] GET `/service-routes/by-path?path=`
- [ ] PUT `/service-routes/{id}`
- [ ] PATCH `/service-routes/{id}/toggle`
- [ ] DELETE `/service-routes/{id}`

#### Rate Limit Management (9 endpoints)
- [ ] POST `/rate-limits`
- [ ] GET `/rate-limits`
- [ ] GET `/rate-limits/{id}`
- [ ] GET `/rate-limits/by-api-key/{apiKeyId}`
- [ ] GET `/rate-limits/by-service-route/{serviceRouteId}`
- [ ] GET `/rate-limits/check?apiKeyId=&serviceRouteId=` ⭐ **CRITICAL**
- [ ] PUT `/rate-limits/{id}`
- [ ] PATCH `/rate-limits/{id}/toggle`
- [ ] DELETE `/rate-limits/{id}`

---

## 🚀 Quick Start Testing Flow

### Step 1: User Management
```
1. POST /api/users/register → Create admin user
2. POST /api/users/signin → Sign in
3. GET /api/users → Verify user created
```

### Step 2: Service Routes
```
4. GET /service-routes → List existing routes
5. POST /service-routes → Create a new route
6. GET /service-routes/{id} → Verify creation
```

### Step 3: API Keys
```
7. POST /api/keys → Create API key for client
8. GET /api/keys → List all keys
9. GET /api/keys/validate?keyValue=xxx → Test validation
```

### Step 4: Rate Limits
```
10. POST /rate-limits → Create specific rate limit
11. POST /rate-limits → Create default route rate limit
12. GET /rate-limits/check?apiKeyId=1&serviceRouteId=2 → Test effective rate limit
```

---

## 🔍 Common Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2026-01-19T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/keys"
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-01-19T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "API Key not found with id: 999",
  "path": "/api/keys/999"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2026-01-19T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/keys"
}
```

---

## 📝 Notes

1. **Port:** All endpoints run on `http://localhost:8082`
2. **CORS:** Enabled for all origins (`@CrossOrigin(origins = "*")`)
3. **Rate Limit Priority:** Specific > Route Default > Key Global > System Default
4. **Date Format:** ISO 8601 format (`2026-01-19T10:30:00`)
5. **Active Status:** Most entities have `isActive` flag for soft delete/disable

---

## 🎯 Next Steps

1. Import this README into Postman as documentation
2. Create a Postman collection with all endpoints
3. Set up environment variables for base URL and common IDs
4. Test each endpoint systematically
5. Document any issues or discrepancies

---

**Last Updated:** January 19, 2026  
**Service Version:** 1.0.0  
**Maintainer:** NexusGate Team
