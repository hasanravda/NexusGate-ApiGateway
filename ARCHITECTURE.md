# 🏗️ NexusGate Architecture

High-level system architecture and design decisions.

## 📊 System Overview

```
┌─────────────┐
│   Clients   │
│ (Web/Mobile)│
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│      NexusGate Gateway (8081)           │
│  ┌────────────────────────────────┐     │
│  │  Filters Chain:                │     │
│  │  1. API Key Validation         │     │
│  │  2. Rate Limiting (Redis)      │     │
│  │  3. JWT Authentication         │     │
│  │  4. Request Logging            │     │
│  └────────────────────────────────┘     │
└──────┬──────────────────────┬───────────┘
       │                      │
       ▼                      ▼
┌─────────────┐      ┌──────────────────┐
│  Analytics  │      │  Backend Services│
│  Service    │      │  (Config, Auth,  │
│  (8084)     │      │   Mock Services) │
└──────┬──────┘      └──────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│        Infrastructure Layer              │
│  ┌──────────┐  ┌───────┐  ┌──────────┐ │
│  │PostgreSQL│  │ Redis │  │Prometheus│ │
│  │  (5432)  │  │ (6379)│  │  (9090)  │ │
│  └──────────┘  └───────┘  └──────────┘ │
└─────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────┐
│   Grafana Dashboard     │
│       (3001)            │
└─────────────────────────┘
```

## 🧩 Core Components

### 1. **NexusGate Gateway** (Port 8081)
- **Technology**: Spring Cloud Gateway (Reactive)
- **Purpose**: Central entry point for all API requests
- **Key Responsibilities**:
  - Request routing based on dynamic database configuration
  - API key validation and management
  - Distributed rate limiting
  - JWT token validation
  - Request/response logging
  - Circuit breaking and resilience

### 2. **Analytics Service** (Port 8084)
- **Technology**: Spring Boot
- **Purpose**: Centralized logging and metrics aggregation
- **Features**:
  - Stores all gateway requests/responses
  - Provides rate limit violations tracking
  - Exposes metrics for Prometheus
  - REST APIs for dashboard consumption

### 3. **Config Service** (Port 8082)
- **Technology**: Spring Boot
- **Purpose**: Configuration and user management
- **Responsibilities**:
  - Service route configuration
  - API key lifecycle management
  - User authentication and authorization

### 4. **Load Tester Service** (Port 8086)
- **Technology**: Spring Boot + WebClient
- **Purpose**: Testing and validation
- **Features**:
  - Scenario-based load testing
  - Rate limit validation
  - Performance benchmarking

### 5. **Mock Backend Services** (Port 8091)
- **Purpose**: Simulates real backend services
- **Endpoints**: Users, Orders, Payments, Inventory

## 💾 Data Layer

### PostgreSQL Database
**Schema Overview:**

```sql
-- Core Tables
1. service_routes       -- Dynamic routing configuration
2. api_keys            -- API key management
3. rate_limits         -- Rate limiting rules
4. users              -- User management
5. api_key_routes     -- API key to route mapping
```

**Key Design Decisions:**
- ✅ Database-driven routing for flexibility
- ✅ Soft deletes with `is_active` flags
- ✅ Composite indexes for query performance
- ✅ Wildcard pattern matching support

### Redis Cache
**Usage:**
- ✅ Distributed rate limiting (Sliding Window)
- ✅ API key caching (TTL: 5 minutes)
- ✅ Route configuration caching
- ✅ Session management

**Key Patterns:**
```
rate_limit:{api_key}:{route}:minute
rate_limit:{api_key}:{route}:hour
rate_limit:{api_key}:{route}:day
api_key:cache:{key_value}
```

## 🔄 Request Flow

### 1. **Incoming Request**
```
Client → Gateway (Port 8081)
```

### 2. **Gateway Filter Chain**
```
Request
  ↓
[1] API Key Validation Filter
  ├─ Check X-API-Key header
  ├─ Validate against DB (with Redis cache)
  └─ Reject if invalid (401)
  ↓
[2] Rate Limiting Filter
  ├─ Check Redis counters
  ├─ Increment counters (minute/hour/day)
  └─ Reject if exceeded (429)
  ↓
[3] JWT Authentication Filter (Optional)
  ├─ Validate Bearer token if present
  └─ Reject if invalid (401)
  ↓
[4] Route Resolution
  ├─ Match request path to service routes
  └─ Apply wildcard patterns
  ↓
[5] Request Logging Filter
  ├─ Log to Analytics Service
  └─ Capture request metadata
  ↓
Backend Service
  ↓
[6] Response Logging Filter
  ├─ Log response to Analytics
  └─ Capture metrics
  ↓
Client
```

## 🎯 Key Design Patterns

### 1. **Reactive Programming**
- Non-blocking I/O with Spring WebFlux
- Reactive streams for high throughput
- Backpressure handling

### 2. **Filter Chain Pattern**
- Ordered filter execution
- Pre and post-filters
- Error handling at each stage

### 3. **Circuit Breaker Pattern**
- Resilience4j integration
- Automatic fallback mechanisms
- Health checks

### 4. **Cache-Aside Pattern**
- Redis for frequently accessed data
- TTL-based invalidation
- Write-through for consistency

### 5. **Database-Driven Configuration**
- Dynamic routing without redeployment
- Runtime configuration updates
- Wildcard pattern matching

## 🔐 Security Architecture

### Multi-Layer Security
```
Layer 1: API Key Authentication
  ├─ Mandatory for gateway routes
  └─ Stored as hashed values

Layer 2: Rate Limiting
  ├─ Per API key per route
  └─ Distributed enforcement (Redis)

Layer 3: JWT Authentication (Optional)
  ├─ Token validation
  └─ Claims extraction

Layer 4: Service-Level Authorization
  └─ Fine-grained access control
```

## 📈 Monitoring & Observability

### Metrics Collection
```
Gateway → Micrometer → Prometheus → Grafana
         → Analytics Service → Dashboard
```

**Key Metrics:**
- Request rate (req/sec)
- Response times (p50, p95, p99)
- Error rates (4xx, 5xx)
- Rate limit violations
- Cache hit rates
- Database connection pool stats

## 🚀 Scalability Considerations

### Horizontal Scaling
- ✅ Stateless gateway instances
- ✅ Redis for shared state
- ✅ PostgreSQL connection pooling
- ✅ Load balancer ready

### Performance Optimizations
- ✅ Redis caching (API keys, routes)
- ✅ Database query optimization
- ✅ Connection pooling (HikariCP)
- ✅ Reactive non-blocking architecture

### Future Enhancements
- 🔄 Distributed tracing (OpenTelemetry)
- 🔄 API versioning support
- 🔄 GraphQL gateway
- 🔄 Kubernetes deployment

## 🛠️ Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Gateway | Spring Cloud Gateway | API routing |
| Backend | Spring Boot 3.x | Microservices |
| Database | PostgreSQL 15 | Persistent storage |
| Cache | Redis 7 | Distributed cache |
| Monitoring | Prometheus + Grafana | Metrics & visualization |
| Frontend | Next.js 14 + React | Admin dashboard |
| Containerization | Docker + Docker Compose | Local development |

## 📚 References

- [Spring Cloud Gateway Docs](https://spring.io/projects/spring-cloud-gateway)
- [Redis Rate Limiting](https://redis.io/docs/manual/patterns/rate-limiter/)
- [Microservices Patterns](https://microservices.io/patterns/)

---

For implementation details, see [backend/PROJECT_DOCUMENTATION.md](backend/PROJECT_DOCUMENTATION.md)
