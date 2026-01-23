# 🚀 NexusGate - Enterprise API Gateway

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud Gateway](https://img.shields.io/badge/Spring%20Cloud%20Gateway-4.0.1-blue.svg)](https://spring.io/projects/spring-cloud-gateway)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()

> **Production-ready distributed API gateway with centralized rate limiting, authentication, and real-time analytics.**

---

## 📋 Quick Links

- [📖 Complete Documentation](PROJECT_DOCUMENTATION.md) - Comprehensive project guide
- [🔌 API Endpoints](ENDPOINTS.md) - All available endpoints
- [⚡ Performance Fixes](PERFORMANCE-FIXES-APPLIED.md) - Optimization details
- [🔄 Filter Execution](nexusgate-gateway/FILTER_EXECUTION_ORDER.md) - Request processing flow
- [🎯 Route-Level Features](ROUTE_LEVEL_API_KEY_FEATURE.md) - API key configuration

---

## 🎯 What is NexusGate?

NexusGate is a **distributed API gateway system** that provides:

- **🔐 API Key Management** - Secure key generation, validation, and lifecycle management
- **⚡ Distributed Rate Limiting** - Redis-based rate limiting across multiple instances
- **🛣️ Dynamic Routing** - Database-driven route configuration (no code changes needed)
- **🔒 Multi-Auth Support** - API Keys, JWT tokens, or both combined
- **📊 Real-Time Analytics** - Request/response logging with Prometheus metrics
- **🧪 Load Testing** - Built-in service for rate limit validation
- **🚀 High Performance** - Non-blocking reactive architecture with in-memory caching

---

## 🏗️ System Architecture

```
┌─────────┐
│ Client  │ (API Key Required)
└────┬────┘
     │ HTTP + X-API-Key
     ↓
┌──────────────────────────────┐
│  Gateway (8081)              │  ← Single Entry Point
│  ├─ Route Resolution         │
│  ├─ API Key Validation       │  ✅ Cache-based (60s refresh)
│  ├─ HTTP Method Check        │  ✅ Per-route enforcement
│  ├─ Authentication           │  ✅ Multi-auth support
│  ├─ Rate Limiting (Redis)    │  ✅ Distributed counters
│  └─ Backend Forwarding       │  ✅ Non-blocking
└──────────────┬───────────────┘
               │
    ┌──────────┼──────────┐
    │          │          │
    ↓          ↓          ↓
┌────────┐ ┌────────┐ ┌────────┐
│Config  │ │Redis   │ │Backend │
│(8082)  │ │(6379)  │ │Services│
└────┬───┘ └────────┘ └────────┘
     │
     ↓
┌──────────┐
│PostgreSQL│
│ (5432)   │
└──────────┘
```

---

## 💻 Microservices

| Service | Port | Purpose | Technology |
|---------|------|---------|------------|
| **Gateway** | 8081 | Core API Gateway | Spring Cloud Gateway 4.0.1 (Reactive) |
| **Config Service** | 8082 | Configuration Management | Spring Boot 4.0.1 (MVC) |
| **Analytics Service** | 8085 | Metrics & Logging | Spring Boot 3.3.7 + Prometheus |
| **Load Tester** | 8083 | Load Testing & Validation | Spring Boot 3.x (WebFlux) |
| **Mock Backend** | 8091 | Test Backend Services | Spring Boot 4.0.1 (MVC) |
| **PostgreSQL** | 5432 | Primary Database | PostgreSQL 17 |
| **Redis** | 6379 | Rate Limiting Cache | Redis 7 |

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** - [Download](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **Docker** - [Download](https://www.docker.com/products/docker-desktop)
- **Docker Compose** - Included with Docker Desktop

### 1. Start Infrastructure

Start PostgreSQL and Redis using Docker Compose:

```powershell
cd backend
docker compose up -d
```

**Verify services are running:**
```powershell
docker ps
# Should show: nexusgate-postgres (port 5432) and nexusgate-redis (port 6379)
```

### 2. Build & Run Services

#### Option A: Run All Services (Recommended Order)

```powershell
# 1. Config Service (Must start first - provides configuration)
cd config-service
mvn clean package -DskipTests
java -jar target/config-service-0.0.1-SNAPSHOT.jar

# 2. Gateway (Depends on Config Service)
cd ../nexusgate-gateway
mvn clean package -DskipTests
java -jar target/nexusgate-gateway-1.0.0.jar

# 3. Analytics Service (Optional - for metrics)
cd ../Analytics-service
mvn clean package -DskipTests
java -jar target/analytics-service-0.0.1-SNAPSHOT.jar

# 4. Mock Backend (Optional - for testing)
cd ../mock-backend-services
mvn clean package -DskipTests
java -jar target/backend-service-0.0.1-SNAPSHOT.jar

# 5. Load Tester (Optional - for load testing)
cd ../load-tester-service
mvn clean package -DskipTests
java -jar target/load-tester-service-0.0.1-SNAPSHOT.jar
```

#### Option B: Use PowerShell Script

```powershell
./start-services-fixed.ps1
```

### 3. Verify Services are Running

```powershell
# Check Gateway
curl http://localhost:8081/actuator/health

# Check Config Service
curl http://localhost:8082/actuator/health

# Check Analytics
curl http://localhost:8085/actuator/health

# Check Mock Backend
curl http://localhost:8091/actuator/health
```

### 4. Make Your First API Request

```powershell
# Test request through gateway
curl -H "X-API-Key: nx_test_key_12345" http://localhost:8081/api/users
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "email": "admin@demo.com",
    "fullName": "Admin User",
    "role": "ADMIN"
  }
]
```

---

## 🔑 Pre-Configured API Keys

The system comes with 4 pre-configured API keys for testing:

| API Key | Client | Rate Limit (minute/hour/day) | Purpose |
|---------|--------|------------------------------|---------|
| `nx_lendingkart_prod_abc123` | LendingKart | 1000 / 60,000 / 1M | Production (high limits) |
| `nx_paytm_prod_xyz789` | PaytmLend | 500 / 30,000 / 500K | Production (medium limits) |
| `nx_mobikwik_test_def456` | MobiKwik | 200 / 10,000 / 200K | Test |
| `nx_test_key_12345` | Test Client | 200 / 10,000 / 200K | Development |

---

## 📍 Available Routes

All routes go through the gateway on port **8081** and require the `X-API-Key` header:

### User Service Routes
```bash
# List all users
curl -H "X-API-Key: nx_test_key_12345" http://localhost:8081/api/users

# Get user by ID
curl -H "X-API-Key: nx_test_key_12345" http://localhost:8081/api/users/1

# Register new user
curl -X POST -H "X-API-Key: nx_test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass123","fullName":"Test User","role":"USER"}' \
  http://localhost:8081/api/users/register
```

### Order Service Routes
```bash
# List all orders
curl -H "X-API-Key: nx_test_key_12345" http://localhost:8081/api/orders

# Create order
curl -X POST -H "X-API-Key: nx_test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-001","productName":"Laptop","quantity":1,"totalAmount":1299.99}' \
  http://localhost:8081/api/orders
```

### Payment Service Routes
```bash
# Process payment
curl -X POST -H "X-API-Key: nx_test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"order-123","amount":199.99,"paymentMethod":"CREDIT_CARD"}' \
  http://localhost:8081/api/payments
```

---

## 🧪 Load Testing

### Run Load Test

```powershell
# Test rate limiting with 150 req/min (should hit 100/min limit)
curl -X POST http://localhost:8083/load-test/start `
  -H "Content-Type: application/json" `
  -d '{
    "targetKey": "nx_test_key_12345",
    "targetEndpoint": "http://localhost:8081/api/users",
    "requestRate": 150,
    "durationSeconds": 60,
    "concurrencyLevel": 10,
    "requestPattern": "CONSTANT_RATE",
    "httpMethod": "GET"
  }'
```

**Response:**
```json
{
  "testId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "message": "Load test started successfully"
}
```

### Check Test Results

```powershell
# Get test status
curl http://localhost:8083/load-test/status/{testId}

# Get final results
curl http://localhost:8083/load-test/results/{testId}
```

---

## 📊 Monitoring & Metrics

### Prometheus Metrics

All services expose metrics at `/actuator/prometheus`:

```powershell
# Gateway metrics
curl http://localhost:8081/actuator/prometheus

# Analytics metrics
curl http://localhost:8085/actuator/prometheus
```

### Key Metrics

- `nexus_requests_total` - Total requests by route
- `nexus_errors_total` - Error count by status code
- `nexus_rate_limit_violations_total` - Rate limit rejections
- `nexus_request_latency` - Request latency (P50, P95, P99)

### Analytics Dashboard

```powershell
# Get 24-hour overview
curl http://localhost:8085/analytics/overview

# Get recent requests (paginated)
curl http://localhost:8085/analytics/recent-requests?page=0&size=20

# Get top endpoints
curl http://localhost:8085/analytics/top-endpoints?limit=10
```

---

## ⚡ Performance Highlights

### Before Optimizations ❌
- Single request: **500-900ms**
- Load (100 req/s): **3.6+ seconds**
- Status: TimeoutException, failures

### After Optimizations ✅
- Single request: **<100ms** (cache hits)
- Load (100 req/s): **<150ms average**
- Config service calls: **Zero during requests**
- Status: **Stable, no timeouts**

### Optimization Techniques

1. **In-Memory Caching** - API keys and routes cached (60s refresh)
2. **Reactive Architecture** - Non-blocking I/O with Spring WebFlux
3. **Redis Connection Pooling** - Reused connections
4. **Filter Ordering** - Early short-circuit for invalid requests
5. **Wildcard Pattern Matching** - Efficient route resolution

---

## 🔧 Configuration

### Gateway Configuration

**File**: `nexusgate-gateway/src/main/resources/application.properties`

```properties
# Server
server.port=8081

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Config Service
config.service.url=http://localhost:8082

# JWT Secret
jwt.secret=your-256-bit-secret-key-change-this-in-production

# Cache Refresh (60 seconds)
# Configured in @Scheduled annotations

# Timeouts
spring.cloud.gateway.httpclient.response-timeout=30s
```

### Database Configuration

**Connection Details**:
- Host: `localhost:5432`
- Database: `nexusgate`
- Username: `nexusgate`
- Password: `nexusgate123`

**Auto-initialized Tables**:
- `users` - System users
- `api_keys` - API key registry
- `service_routes` - Route configurations
- `rate_limits` - Rate limit policies
- `request_logs` - Analytics logs
- `metrics_summary` - Aggregated metrics

---

## 🛠️ Development

### Project Structure

```
backend/
├── nexusgate-gateway/       # API Gateway (Port 8081)
├── config-service/          # Configuration Service (Port 8082)
├── Analytics-service/       # Analytics & Metrics (Port 8085)
├── load-tester-service/     # Load Testing (Port 8083)
├── mock-backend-services/   # Mock Backends (Port 8091)
├── db/                      # Database scripts
│   ├── init-db.sql         # Schema & seed data
│   └── *.sql               # Migration scripts
├── docker-compose.yml       # Infrastructure setup
└── PROJECT_DOCUMENTATION.md # Complete documentation
```

### Adding a New Route

#### Via Config Service API

```powershell
curl -X POST http://localhost:8082/service-routes `
  -H "Content-Type: application/json" `
  -d '{
    "serviceName": "notification-service",
    "serviceDescription": "Send notifications",
    "publicPath": "/api/notifications/**",
    "targetUrl": "http://localhost:8092/notifications",
    "allowedMethods": ["POST", "GET"],
    "authRequired": true,
    "authType": "API_KEY",
    "requiresApiKey": true,
    "rateLimitEnabled": true,
    "rateLimitPerMinute": 50,
    "rateLimitPerHour": 2000,
    "timeoutMs": 30000,
    "isActive": true,
    "createdByUserId": 1
  }'
```

**Gateway will pick up the new route within 60 seconds** (automatic cache refresh).

### Creating a New API Key

```powershell
curl -X POST http://localhost:8082/api/keys `
  -H "Content-Type: application/json" `
  -d '{
    "keyName": "Production Key for Acme Corp",
    "clientName": "Acme Corporation",
    "clientEmail": "api@acme.com",
    "clientCompany": "Acme Corp",
    "createdByUserId": 1,
    "expiresAt": "2027-12-31T23:59:59"
  }'
```

**Response:**
```json
{
  "id": 5,
  "keyValue": "nx_acme_prod_xyz123abc",
  "keyName": "Production Key for Acme Corp",
  "isActive": true,
  "createdAt": "2026-01-23T10:30:00"
}
```

---

## 🐛 Troubleshooting

### Gateway Not Starting

**Issue**: Gateway fails to start or can't connect to Config Service

**Solutions**:
1. Ensure Config Service is running: `curl http://localhost:8082/actuator/health`
2. Check Redis is running: `docker ps | grep redis`
3. Verify Java 21 is installed: `java -version`

### 401 Unauthorized

**Issue**: Requests return 401 even with valid API key

**Solutions**:
1. Verify API key exists: `curl http://localhost:8082/api/keys`
2. Check API key is active: `isActive: true`
3. Verify header name is `X-API-Key` (case-sensitive)
4. Check API key hasn't expired

### 429 Too Many Requests

**Issue**: Rate limit exceeded

**Solutions**:
1. Expected behavior when limits are exceeded
2. Check current limits: `curl "http://localhost:8082/rate-limits/check?apiKeyId=1&serviceRouteId=1"`
3. Wait for time window to reset (1 minute for per-minute limits)
4. Request higher limits if needed

### 404 Not Found

**Issue**: Route not found

**Solutions**:
1. Verify route exists: `curl http://localhost:8082/service-routes`
2. Check route is active: `isActive: true`
3. Verify path pattern matches: `/api/users/**` matches `/api/users`, `/api/users/123`, etc.
4. Wait 60 seconds for cache refresh or restart gateway

### 502 Bad Gateway

**Issue**: Backend service unreachable

**Solutions**:
1. Verify target service is running
2. Check target URL in route config
3. Test direct connection: `curl http://localhost:8091/users`
4. Check network connectivity and firewall rules

---

## 📚 Additional Resources

### Documentation Files

- [📖 PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md) - Complete project documentation
- [🔌 ENDPOINTS.md](ENDPOINTS.md) - All API endpoints
- [⚡ PERFORMANCE-FIXES-APPLIED.md](PERFORMANCE-FIXES-APPLIED.md) - Performance optimization details
- [🔄 nexusgate-gateway/FILTER_EXECUTION_ORDER.md](nexusgate-gateway/FILTER_EXECUTION_ORDER.md) - Filter chain flow
- [🎯 ROUTE_LEVEL_API_KEY_FEATURE.md](ROUTE_LEVEL_API_KEY_FEATURE.md) - Route-level features
- [🏗️ Analytics-service/ARCHITECTURE.md](Analytics-service/ARCHITECTURE.md) - Analytics architecture
- [🧪 load-tester-service/ARCHITECTURE.md](load-tester-service/ARCHITECTURE.md) - Load testing details

### Service-Specific READMEs

- [Gateway README](nexusgate-gateway/README.md)
- [Config Service README](config-service/README.md)
- [Analytics Service README](Analytics-service/README.md)
- [Load Tester README](load-tester-service/README.md)
- [Mock Backend README](mock-backend-services/README.md)

---

## 🎯 Use Cases

### Use Case 1: FinTech API Platform
**LendingKart** provides APIs to 100+ partners:
- Each partner gets a unique API key
- Different rate limits based on subscription tier
- Real-time usage tracking for billing
- Multi-level security (API Key + JWT)

### Use Case 2: SaaS Platform
Multiple backend microservices with:
- Centralized authentication
- Unified rate limiting
- Request/response analytics
- Easy route management

### Use Case 3: Public API Gateway
Expose public and private APIs:
- Some routes require authentication
- Others are public (documentation, health checks)
- Per-client rate limiting
- Usage analytics for optimization

---

## 🤝 Contributing

### Development Workflow

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Write/update tests
5. Commit: `git commit -m 'Add amazing feature'`
6. Push: `git push origin feature/amazing-feature`
7. Open a Pull Request

### Code Standards

- Follow Spring Boot best practices
- Use Lombok for boilerplate reduction
- Write comprehensive JavaDoc comments
- Include unit tests for new features
- Update relevant documentation

---

## 📄 License

This project is proprietary software. All rights reserved.

---

## 🙏 Acknowledgments

- **Spring Boot & Spring Cloud** - Excellent framework
- **Redis Labs** - Robust caching solution
- **PostgreSQL** - Reliable database
- **Docker** - Simplified deployment

---

## 📞 Support

For issues, questions, or feature requests:
- Review documentation files in each service directory
- Check troubleshooting section above
- Review existing code and comments

---

**Built with ❤️ using Spring Boot, Spring Cloud Gateway, Redis, and PostgreSQL**

**Last Updated**: January 23, 2026 | **Version**: 1.0.0
