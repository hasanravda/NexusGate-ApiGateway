# 🚀 NexusGate API Gateway

**A distributed API rate limiting gateway built with Spring Cloud Gateway 4.0.1 (Reactive)**

## 📋 Overview

NexusGate is a high-performance, fully reactive API gateway that provides:
- ✅ Dynamic routing based on database configuration
- ✅ API Key authentication
- ✅ JWT token validation
- ✅ Distributed rate limiting using Redis
- ✅ Custom header injection
- ✅ Request/response logging
- ✅ Circuit breaking with timeouts

---

## 🏗️ Technology Stack

- **Spring Boot**: 3.3.7
- **Spring Cloud Gateway**: 4.0.1 (via Spring Cloud 2023.0.4)
- **Java**: 21
- **Redis**: For distributed rate limiting
- **WebFlux**: Fully reactive, non-blocking I/O
- **JWT**: Token-based authentication
- **Lombok**: Reduce boilerplate code

---

## 🔧 Configuration

### Application Properties (`application.properties`)

```properties
# Server Configuration
server.port=8081

# Spring Cloud Gateway
spring.cloud.gateway.enabled=true
spring.cloud.gateway.discovery.locator.enabled=false

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.database=0
spring.data.redis.timeout=2000ms

# JWT Configuration
jwt.secret=your-256-bit-secret-key-change-this-in-production-must-be-at-least-32-chars

# Config Service URL
config.service.url=http://localhost:8082

# Logging
logging.level.com.nexusgate.gateway=DEBUG
logging.level.org.springframework.cloud.gateway=DEBUG
```

---

## 🌐 API Endpoints

### Gateway Endpoints

The gateway **dynamically routes** all requests based on configuration. It doesn't expose its own REST endpoints but acts as a proxy.

#### Request Flow Pattern:
```
Client → Gateway (8081) → Config Service (8082) → Backend Service
```

### Example 1: User Service Request

**Client Request:**
```http
GET http://localhost:8081/api/users
X-API-KEY: abc123xyz
```

**What Happens:**
1. Gateway receives request at port 8081
2. Queries config service: `GET http://localhost:8082/service-routes/by-path?path=/api/users`
3. Gets route config with target URL: `http://user-service:8080`
4. Validates API key via: `GET http://localhost:8082/api/keys/validate?keyValue=abc123xyz`
5. Checks rate limits via: `GET http://localhost:8082/rate-limits/check?apiKeyId=123&serviceRouteId=1`
6. Forwards to: `GET http://user-service:8080/api/users`
7. Returns response to client

---

### Example 2: Order Service Request with JWT

**Client Request:**
```http
POST http://localhost:8081/api/orders
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "productId": 123,
  "quantity": 2
}
```

**What Happens:**
1. Gateway validates JWT signature and expiration
2. Routes to order service based on `/api/orders` path
3. Checks rate limits
4. Forwards to: `POST http://order-service:8080/api/orders`
5. Returns response

---

### Example 3: Request with Custom Headers

**Client Request:**
```http
GET http://localhost:8081/api/products
X-API-KEY: abc123xyz
```

**Gateway Processing:**
- Adds custom headers from route config (e.g., `X-Custom-Header: value`)
- Injects `X-NexusGate-ApiKey-Id: 123`
- Injects `X-NexusGate-ServiceRoute-Id: 1`
- Forwards with all headers to target service

---

## 🔌 Required Config Service Endpoints

Your Config Service (port 8082) **MUST** implement these endpoints:

### 1. Get Route by Path

**Endpoint:**
```http
GET http://localhost:8082/service-routes/by-path?path={requestPath}
```

**Example:**
```http
GET http://localhost:8082/service-routes/by-path?path=/api/users
```

**Response:**
```json
{
  "id": 1,
  "publicPath": "/api/users",
  "targetUrl": "http://user-service:8080",
  "allowedMethods": ["GET", "POST", "PUT", "DELETE"],
  "authRequired": true,
  "authType": "API_KEY",
  "rateLimitEnabled": true,
  "rateLimitPerMinute": 60,
  "rateLimitPerHour": 1000,
  "timeoutMs": 30000,
  "customHeaders": "{\"X-Custom\":\"value\",\"X-Region\":\"US\"}",
  "isActive": true
}
```

**Fields:**
- `id`: Route identifier
- `publicPath`: Path pattern to match (supports wildcards)
- `targetUrl`: Backend service URL
- `allowedMethods`: HTTP methods allowed
- `authRequired`: Whether authentication is required
- `authType`: `API_KEY`, `JWT`, or `BOTH`
- `rateLimitEnabled`: Enable rate limiting
- `rateLimitPerMinute`: Max requests per minute
- `rateLimitPerHour`: Max requests per hour
- `timeoutMs`: Request timeout in milliseconds
- `customHeaders`: JSON string of headers to inject
- `isActive`: Route enabled/disabled

---

### 2. Validate API Key

**Endpoint:**
```http
GET http://localhost:8082/api/keys/validate?keyValue={apiKey}
```

**Example:**
```http
GET http://localhost:8082/api/keys/validate?keyValue=abc123xyz
```

**Response:**
```json
{
  "id": 123,
  "keyValue": "abc123xyz",
  "isActive": true,
  "expiresAt": "2026-12-31T23:59:59"
}
```

**Fields:**
- `id`: API key ID (used for rate limiting)
- `keyValue`: The actual API key string
- `isActive`: Whether key is active
- `expiresAt`: Expiration timestamp (optional)

---

### 3. Check Rate Limit

**Endpoint:**
```http
GET http://localhost:8082/rate-limits/check?apiKeyId={keyId}&serviceRouteId={routeId}
```

**Example:**
```http
GET http://localhost:8082/rate-limits/check?apiKeyId=123&serviceRouteId=1
```

**Response:**
```json
{
  "isActive": true,
  "requestsPerMinute": 60,
  "requestsPerHour": 1000
}
```

**Fields:**
- `isActive`: Whether rate limiting is active
- `requestsPerMinute`: Max requests per minute
- `requestsPerHour`: Max requests per hour

---

## 🔄 Request Processing Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                      Client Request                              │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              Spring Cloud Gateway (Port 8081)                    │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│  Filter 1: GlobalRequestFilter (Order -100)                      │
│  • Extract request path, method, client IP                       │
│  • Call Config Service: GET /service-routes/by-path             │
│  • Validate route is active                                      │
│  • Store route in exchange attributes                            │
│  • Log request start                                             │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│  Filter 2: AuthenticationFilter (Order -90)                      │
│  • Check if authentication required                              │
│  • Extract X-API-KEY or Authorization header                     │
│  • Validate API Key via Config Service                           │
│  • Validate JWT signature and expiration                         │
│  • Store apiKeyId in exchange                                    │
│  • Return 401 if authentication fails                            │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│  Filter 3: RateLimitFilter (Order -80)                          │
│  • Check if rate limiting enabled                                │
│  • Call Config Service: GET /rate-limits/check                  │
│  • Check Redis counters (minute and hour)                        │
│  • Increment counters atomically                                 │
│  • Return 429 if rate limit exceeded                             │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│  Filter 4: ServiceRoutingFilter (Order -70)                     │
│  • Get route from exchange attributes                            │
│  • Build headers (original + custom)                             │
│  • Inject X-NexusGate-ApiKey-Id                                 │
│  • Inject X-NexusGate-ServiceRoute-Id                           │
│  • Create WebClient with target URL                              │
│  • Stream request body (non-blocking)                            │
│  • Forward to backend service                                    │
│  • Stream response back to client                                │
│  • Handle errors (502 Bad Gateway)                               │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Backend Service Response                        │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Client Response                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Authentication Types

### 1. API Key Authentication

**Header:**
```
X-API-KEY: abc123xyz
```

**Use Case:** External API integrations, service-to-service communication

**Example:**
```bash
curl -X GET http://localhost:8081/api/users \
  -H "X-API-KEY: abc123xyz"
```

---

### 2. JWT Authentication

**Header:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Use Case:** User-based authentication, mobile apps, web applications

**Example:**
```bash
curl -X GET http://localhost:8081/api/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 3. Both API Key + JWT

**Headers:**
```
X-API-KEY: abc123xyz
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Use Case:** High-security endpoints requiring both service and user authentication

**Example:**
```bash
curl -X POST http://localhost:8081/api/payments \
  -H "X-API-KEY: abc123xyz" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00}'
```

---

## 📊 Rate Limiting

### How It Works

1. **Redis-based counters** track requests per API key per route
2. **Two buckets**: Minute and Hour
3. **Atomic increments** ensure distributed consistency
4. **Automatic TTL** on counters (60s for minute, 3600s for hour)

### Redis Key Structure

```
nexusgate:{apiKeyId}:{serviceRouteId}:minute  → Counter (TTL: 60s)
nexusgate:{apiKeyId}:{serviceRouteId}:hour    → Counter (TTL: 3600s)
```

**Example:**
```
nexusgate:123:1:minute = 45   (45 requests this minute)
nexusgate:123:1:hour = 789    (789 requests this hour)
```

### Rate Limit Response

When exceeded:
```json
{
  "timestamp": 1705659084000,
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded",
  "path": "/api/users"
}
```

---

## 🚀 Running the Application

### Prerequisites

1. **Java 21** installed
2. **Redis** running on localhost:6379
3. **Config Service** running on localhost:8082

### Start Redis

```bash
# Using Docker
docker run -d -p 6379:6379 redis:latest

# Or using Redis CLI
redis-server
```

### Run Gateway

**Option 1: IntelliJ IDEA**
1. Open project in IntelliJ
2. Right-click `NexusgateGatewayApplication.java`
3. Select "Run"

**Option 2: Command Line (if Maven is installed)**
```bash
mvn spring-boot:run
```

**Option 3: Build and Run JAR**
```bash
mvn clean package
java -jar target/nexusgate-gateway-1.0.0.jar
```

---

## 🧪 Testing the Gateway

### Test 1: Simple Request (No Auth)

```bash
curl -X GET http://localhost:8081/api/public/health
```

---

### Test 2: API Key Authentication

```bash
curl -X GET http://localhost:8081/api/users \
  -H "X-API-KEY: abc123xyz"
```

**Expected:** 200 OK (if valid) or 401 Unauthorized (if invalid)

---

### Test 3: JWT Authentication

```bash
curl -X GET http://localhost:8081/api/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Test 4: Rate Limiting Test

```bash
# Send 100 requests rapidly
for i in {1..100}; do
  curl -X GET http://localhost:8081/api/users \
    -H "X-API-KEY: abc123xyz"
  echo "Request $i"
done
```

**Expected:** First 60 succeed, then 429 Too Many Requests

---

### Test 5: POST with JSON Body

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "X-API-KEY: abc123xyz" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 123,
    "quantity": 2,
    "price": 99.99
  }'
```

---

## 📝 Error Responses

### 401 Unauthorized
```json
{
  "timestamp": 1705659084000,
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid API key",
  "path": "/api/users"
}
```

### 404 Not Found
```json
{
  "timestamp": 1705659084000,
  "status": 404,
  "error": "Not Found",
  "message": "Route not found or inactive",
  "path": "/api/unknown"
}
```

### 429 Too Many Requests
```json
{
  "timestamp": 1705659084000,
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded",
  "path": "/api/users"
}
```

### 502 Bad Gateway
```json
{
  "timestamp": 1705659084000,
  "status": 502,
  "error": "Bad Gateway",
  "message": "Error forwarding request",
  "path": "/api/users"
}
```

---

## 🔍 Monitoring & Logging

### Log Levels

```properties
logging.level.com.nexusgate.gateway=DEBUG
logging.level.org.springframework.cloud.gateway=DEBUG
```

### Sample Log Output

```
2026-01-19 14:30:45.123 INFO  GlobalRequestFilter : Incoming request: GET /api/users from 192.168.1.100
2026-01-19 14:30:45.145 DEBUG AuthenticationFilter : API key validated successfully for keyId: 123
2026-01-19 14:30:45.156 DEBUG RateLimitFilter : Rate limit check: 45/60 requests this minute
2026-01-19 14:30:45.234 INFO  GlobalRequestFilter : Request completed: GET /api/users - Status: 200 - Duration: 111ms - ApiKeyId: 123 - RouteId: 1
```

---

## 🏛️ Project Structure

```
nexusgate-gateway/
├── src/main/java/com/nexusgate/gateway/
│   ├── NexusgateGatewayApplication.java       # Main application
│   ├── config/
│   │   ├── GatewayConfig.java                 # Gateway & WebClient config
│   │   ├── RedisConfig.java                   # Redis template config
│   │   ├── JacksonConfig.java                 # JSON serialization
│   │   └── SecurityConfig.java                # Spring Security config
│   ├── filter/
│   │   ├── GlobalRequestFilter.java           # Route discovery (-100)
│   │   ├── AuthenticationFilter.java          # Auth validation (-90)
│   │   ├── RateLimitFilter.java              # Rate limiting (-80)
│   │   └── ServiceRoutingFilter.java         # Request forwarding (-70)
│   ├── client/
│   │   ├── ServiceRouteClient.java           # Fetch route config
│   │   ├── ApiKeyClient.java                 # Validate API keys
│   │   └── RateLimitClient.java              # Fetch rate limits
│   ├── dto/
│   │   ├── ServiceRouteResponse.java         # Route metadata
│   │   ├── ApiKeyResponse.java               # API key info
│   │   └── EffectiveRateLimitResponse.java   # Rate limit config
│   ├── redis/
│   │   └── RedisRateLimiterService.java      # Redis rate limiting
│   ├── security/
│   │   └── JwtValidator.java                 # JWT validation
│   ├── util/
│   │   ├── HeaderUtil.java                   # Header extraction
│   │   └── PathMatcherUtil.java              # Path matching
│   └── exception/
│       └── GatewayExceptionHandler.java      # Global error handler
├── src/main/resources/
│   └── application.properties                 # Configuration
├── pom.xml                                    # Maven dependencies
└── README.md                                  # This file
```

---

## 🎯 Key Features

✅ **Fully Reactive** - Non-blocking I/O with Project Reactor  
✅ **Dynamic Routing** - Routes stored in database, no restart needed  
✅ **Distributed Rate Limiting** - Redis-based, works across multiple instances  
✅ **Flexible Authentication** - API Key, JWT, or both  
✅ **Custom Headers** - Inject headers per route  
✅ **Request Logging** - Track duration, status, and metadata  
✅ **Error Handling** - Graceful failures with proper HTTP status codes  
✅ **Circuit Breaking** - Configurable timeouts per route  
✅ **Path Matching** - Supports wildcards and patterns  
✅ **Client IP Detection** - Handles X-Forwarded-For  

---

## 📦 Dependencies

### Core
- Spring Boot 3.3.7
- Spring Cloud Gateway 4.0.1
- Spring WebFlux
- Spring Data Redis Reactive

### Security
- Spring Security
- JJWT (JWT library) 0.12.6

### Utilities
- Lombok
- Jackson (JSON processing)

### Testing
- Spring Boot Test
- Reactor Test
- Spring Security Test

---

## 🔧 Advanced Configuration

### Custom Timeout per Route

Configure in route metadata:
```json
{
  "timeoutMs": 5000
}
```

### Custom Headers per Route

Configure as JSON string:
```json
{
  "customHeaders": "{\"X-Region\":\"US\",\"X-Version\":\"v2\"}"
}
```

### Rate Limiting Strategy

Configure per API key and route combination:
- Minute limits: Short-burst protection
- Hour limits: Long-term throttling

---

## 🚨 Troubleshooting

### Gateway not starting
- ✅ Check Java 21 is installed: `java -version`
- ✅ Check Redis is running: `redis-cli ping`
- ✅ Check Config Service is accessible: `curl http://localhost:8082`

### 404 Route Not Found
- ✅ Verify route exists in Config Service
- ✅ Check route is active (`isActive: true`)
- ✅ Verify path matches exactly

### 401 Unauthorized
- ✅ Check API key is valid in Config Service
- ✅ Verify API key not expired
- ✅ Check header name is `X-API-KEY` (case-sensitive)

### 429 Too Many Requests
- ✅ Expected behavior when limits exceeded
- ✅ Check Redis counters: `redis-cli KEYS nexusgate:*`
- ✅ Wait for minute/hour window to reset

### 502 Bad Gateway
- ✅ Check target service is running
- ✅ Verify target URL in route config
- ✅ Check network connectivity

---

## 📄 License

This project is part of the NexusGate distributed API rate limiting system.

---

## 👥 Support

For issues or questions, please create an issue in the repository.

---

**Built with ❤️ using Spring Cloud Gateway**
