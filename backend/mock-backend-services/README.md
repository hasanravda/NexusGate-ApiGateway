# Backend Service (Mock Services)

Mock backend services for testing NexusGate API Gateway. Includes user-service, order-service, and payment-service with Prometheus metrics.

## Overview

This service provides three mock backend APIs:
- **User Service** - User management operations
- **Order Service** - Order processing operations  
- **Payment Service** - Payment processing with simulated failures

**Purpose**: Demonstrate API gateway routing, rate limiting, load testing, and monitoring without complex infrastructure.

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.1** (MVC - Blocking)
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer Prometheus** - Prometheus-compatible metrics
- **In-memory storage** - ConcurrentHashMap (no database)

## Quick Start

### Build
```bash
./mvnw clean package
```

### Run
```bash
java -jar target/backend-service-0.0.1-SNAPSHOT.jar
```

Service runs on: **http://localhost:8091**

## API Endpoints

### 👤 User Service

**Base Path**: `/users`

| Method | Endpoint | Description | Delay |
|--------|----------|-------------|-------|
| GET | `/users` | List all users | 50-150ms |
| GET | `/users/{id}` | Get user by ID | 50-100ms |
| POST | `/users` | Create new user | 100-200ms |
| PUT | `/users/{id}` | Update user | 80-150ms |
| DELETE | `/users/{id}` | Delete user | 60-120ms |

**Example Request:**
```bash
# Get all users
curl http://localhost:8091/users

# Create user
curl -X POST http://localhost:8091/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "fullName": "John Doe",
    "role": "USER"
  }'
```

**Metrics:**
- `mock_users_total` - Total users created

---

### 📦 Order Service

**Base Path**: `/orders`

| Method | Endpoint | Description | Delay |
|--------|----------|-------------|-------|
| GET | `/orders` | List all orders | 100-200ms |
| GET | `/orders/{id}` | Get order by ID | 100-200ms |
| POST | `/orders` | Create new order | 100-300ms |
| GET | `/orders/user/{userId}` | Get orders by user | 120-250ms |
| DELETE | `/orders/{id}` | Cancel order | 100-200ms |

**Example Request:**
```bash
# Get all orders
curl http://localhost:8091/orders

# Create order
curl -X POST http://localhost:8091/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-001",
    "productName": "Laptop",
    "quantity": 1,
    "totalAmount": 1299.99
  }'
```

**Metrics:**
- `mock_orders_total` - Total orders created

---

### 💳 Payment Service

**Base Path**: `/payments`

| Method | Endpoint | Description | Delay | Failure Rate |
|--------|----------|-------------|-------|--------------|
| GET | `/payments` | List all payments | 300-500ms | - |
| GET | `/payments/{id}` | Get payment by ID | 300-500ms | - |
| POST | `/payments` | Process payment | 300-700ms | **~10%** |
| GET | `/payments/order/{orderId}` | Get payment by order | 300-600ms | - |

**Example Request:**
```bash
# Get all payments
curl http://localhost:8091/payments

# Process payment (may fail randomly)
curl -X POST http://localhost:8091/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-123",
    "amount": 199.99,
    "paymentMethod": "CREDIT_CARD"
  }'
```

**Metrics:**
- `mock_payments_success_total` - Successful payments
- `mock_payments_failed_total` - Failed payments

**Failure Reasons:**
- Insufficient funds
- Card declined
- Payment timeout
- Invalid card details
- Bank server unavailable

---

## Monitoring & Metrics

### Actuator Endpoints

All endpoints available at: **http://localhost:8091/actuator**

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Service health status |
| `/actuator/info` | Application info |
| `/actuator/prometheus` | Prometheus metrics |

### Prometheus Metrics

**Custom Business Metrics:**
```
# User service
mock_users_total{service="user-service"}

# Order service  
mock_orders_total{service="order-service"}

# Payment service
mock_payments_success_total{service="payment-service",status="success"}
mock_payments_failed_total{service="payment-service",status="failed"}
```

**Standard HTTP Metrics** (auto-generated):
```
http_server_requests_seconds_count{uri="/users",method="GET",status="200"}
http_server_requests_seconds_sum{uri="/users",method="GET",status="200"}
jvm_memory_used_bytes{area="heap"}
process_cpu_usage
```

### View Metrics

```bash
# All Prometheus metrics
curl http://localhost:8091/actuator/prometheus

# Filter custom metrics
curl http://localhost:8091/actuator/prometheus | grep "mock_"

# Health check
curl http://localhost:8091/actuator/health
```

---

## Configuration

**application.yml:**
```yaml
server:
  port: 8091

management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## Features

### ✅ Realistic Behavior
- Simulated processing delays
- Random payment failures (~10%)
- Predictable responses

### ✅ Observability
- Prometheus-compatible metrics
- Health checks
- Detailed logging

### ✅ Simplicity
- No database required
- In-memory storage
- Easy to run and test

### ✅ Interview-Ready
- Clean, readable code
- Well-documented
- Demonstrates best practices

---

## Testing with Gateway

To test through NexusGate API Gateway:

```bash
# Via Gateway (requires API key)
curl http://localhost:8080/api/users \
  -H "X-API-Key: nx_lendingkart_prod_abc123"

# Direct to backend (no auth)
curl http://localhost:8091/users
```

**⚠️ Important: Database Configuration Required**

Before testing, ensure service routes in database point to backend-service:
```bash
# Execute the SQL update script
docker exec -i <postgres-container> psql -U nexusgate -d nexusgate_db < infrastructure/db/update-service-routes.sql

# Or manually via docker-compose
cd infrastructure
docker-compose exec postgres psql -U nexusgate -d nexusgate_db -f /docker-entrypoint-initdb.d/update-service-routes.sql
```

**Expected Service Routes:**
- User Service: `http://localhost:8091/users`
- Order Service: `http://localhost:8091/orders`
- Payment Service: `http://localhost:8091/payments`

---

## Load Testing

Use with load-tester-service:

```bash
POST http://localhost:8083/load-test/start
{
  "targetEndpoint": "http://localhost:8091/payments",
  "requestRate": 100,
  "durationSeconds": 60
}
```

Monitor metrics during load test:
```bash
watch -n 1 'curl -s http://localhost:8091/actuator/prometheus | grep mock_'
```

---

## Architecture Notes

### Why Mock Services?

1. **Testing** - Validate gateway routing without real backend
2. **Rate Limiting** - Demonstrate rate limiter effectiveness
3. **Monitoring** - Show metrics collection and visualization
4. **Load Testing** - Stress test gateway with predictable load
5. **Demo** - Interview-ready showcase project

### Design Decisions

- **In-memory storage**: No database complexity
- **Blocking I/O**: Simpler than reactive (Spring MVC not WebFlux)
- **Simulated delays**: Realistic latency for testing
- **Random failures**: Demonstrate error handling
- **Custom metrics**: Show business metric tracking

---

## Integration with Prometheus/Grafana

### Prometheus Configuration

Add to `prometheus.yml`:
```yaml
scrape_configs:
  - job_name: 'backend-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8091']
```

### Grafana Dashboard Queries

**Request Rate:**
```promql
rate(http_server_requests_seconds_count{application="backend-service"}[1m])
```

**Payment Success Rate:**
```promql
rate(mock_payments_success_total[5m]) / (rate(mock_payments_success_total[5m]) + rate(mock_payments_failed_total[5m]))
```

**User Creation Rate:**
```promql
rate(mock_users_total[1m])
```

**P95 Latency:**
```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

---

## Troubleshooting

### Service won't start

Check if port 8091 is already in use:
```bash
lsof -i :8091
kill -9 <PID>
```

### Metrics not appearing

1. Verify actuator is enabled:
   ```bash
   curl http://localhost:8091/actuator
   ```

2. Check Prometheus endpoint:
   ```bash
   curl http://localhost:8091/actuator/prometheus
   ```

### High memory usage

Restart service to clear in-memory data:
```bash
pkill -f backend-service
java -jar target/backend-service-0.0.1-SNAPSHOT.jar
```

---

## Future Enhancements

- [ ] Add circuit breaker simulation
- [ ] Configurable failure rates
- [ ] More realistic data generation
- [ ] Database option (H2)
- [ ] Docker containerization
- [ ] Multiple instances for load balancing

---

## License

Part of NexusGate API Gateway project - Educational/Demo purposes
