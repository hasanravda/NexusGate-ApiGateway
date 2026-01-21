# Mock Backend Services - Implementation Summary

## ✅ What Was Implemented

### 1. **Three Mock Services in One Application**
- **User Service** (`/users`)
- **Order Service** (`/orders`)  
- **Payment Service** (`/payments`)

All services run on **port 8091** in a single Spring Boot application.

---

## 📊 Prometheus Metrics

### Custom Business Metrics

| Metric Name | Type | Description | Tags |
|-------------|------|-------------|------|
| `mock_users_total` | Counter | Total users created | service=user-service |
| `mock_orders_total` | Counter | Total orders created | service=order-service |
| `mock_payments_success_total` | Counter | Successful payments | service=payment-service, status=success |
| `mock_payments_failed_total` | Counter | Failed payments | service=payment-service, status=failed |

### Auto-Generated HTTP Metrics

Spring Boot Actuator + Micrometer automatically provides:

```
# Request counts and timing
http_server_requests_seconds_count{uri="/users",method="GET",status="200"}
http_server_requests_seconds_sum{uri="/users",method="GET",status="200"}

# Active requests
http_server_requests_active_seconds_count

# JVM metrics
jvm_memory_used_bytes{area="heap"}
jvm_threads_states_threads{state="runnable"}
process_cpu_usage

# System metrics
system_cpu_usage
system_cpu_count
```

---

## 🎯 Key Features

### ✅ Realistic Behavior
- **Simulated delays**: User (50-200ms), Order (100-300ms), Payment (300-700ms)
- **Random failures**: Payment service has ~10% failure rate
- **Failure reasons**: "Insufficient funds", "Card declined", "Payment timeout", etc.

### ✅ Observability
- **Prometheus endpoint**: `/actuator/prometheus`
- **Health checks**: `/actuator/health`
- **Info endpoint**: `/actuator/info`
- **Custom metrics**: Track business operations

### ✅ Interview-Ready
- Clean, documented code
- In-memory storage (ConcurrentHashMap)
- No external dependencies (database, cache)
- Easy to explain and demonstrate

---

## 🔧 Technical Stack

```
Java 21
Spring Boot 4.0.1
Spring MVC (Blocking I/O)
Spring Boot Actuator
Micrometer Prometheus Registry
Lombok
```

---

## 📝 API Summary

### User Service
```bash
GET    /users           # List users (50-150ms)
GET    /users/{id}      # Get user (50-100ms)
POST   /users           # Create user (100-200ms) ← Increments metric
PUT    /users/{id}      # Update user (80-150ms)
DELETE /users/{id}      # Delete user (60-120ms)
```

### Order Service
```bash
GET    /orders              # List orders (100-200ms)
GET    /orders/{id}         # Get order (100-200ms)
POST   /orders              # Create order (100-300ms) ← Increments metric
GET    /orders/user/{id}    # Orders by user (120-250ms)
DELETE /orders/{id}         # Cancel order (100-200ms)
```

### Payment Service
```bash
GET    /payments                # List payments (300-500ms)
GET    /payments/{id}           # Get payment (300-500ms)
POST   /payments                # Process payment (300-700ms) ← Increments success/failed metric
GET    /payments/order/{id}     # Payment by order (300-600ms)
```

---

## 🚀 Usage

### Start Service
```bash
cd backend/backend-service
java -jar target/backend-service-0.0.1-SNAPSHOT.jar
```

### Test Endpoints
```bash
# Create user
curl -X POST http://localhost:8091/users \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","fullName":"John Doe","role":"USER"}'

# Create order
curl -X POST http://localhost:8091/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-001","productName":"Laptop","quantity":1,"totalAmount":1299.99}'

# Process payment (may fail randomly)
curl -X POST http://localhost:8091/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":"order-123","amount":199.99,"paymentMethod":"CREDIT_CARD"}'
```

### View Metrics
```bash
# All Prometheus metrics
curl http://localhost:8091/actuator/prometheus

# Custom metrics only
curl http://localhost:8091/actuator/prometheus | grep "mock_"

# Health check
curl http://localhost:8091/actuator/health
```

### Run Test Script
```bash
./test-backend.sh
```

---

## 📈 Integration with Monitoring

### Prometheus Configuration
```yaml
scrape_configs:
  - job_name: 'backend-service'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:8091']
```

### Grafana Dashboard Queries

**User Creation Rate:**
```promql
rate(mock_users_total[1m])
```

**Order Creation Rate:**
```promql
rate(mock_orders_total[1m])
```

**Payment Success Rate:**
```promql
rate(mock_payments_success_total[5m]) / 
(rate(mock_payments_success_total[5m]) + rate(mock_payments_failed_total[5m]))
```

**Request Rate by Endpoint:**
```promql
rate(http_server_requests_seconds_count{application="backend-service"}[1m])
```

**P95 Latency:**
```promql
histogram_quantile(0.95, 
  rate(http_server_requests_seconds_bucket{application="backend-service"}[5m])
)
```

**Average Response Time:**
```promql
rate(http_server_requests_seconds_sum{application="backend-service"}[5m]) /
rate(http_server_requests_seconds_count{application="backend-service"}[5m])
```

---

## 🧪 Load Testing

Use these endpoints with the load-tester-service:

```bash
# Test user service
POST http://localhost:8083/load-test/start
{
  "targetEndpoint": "http://localhost:8091/users",
  "requestRate": 100,
  "durationSeconds": 60
}

# Test payment service (with failures)
POST http://localhost:8083/load-test/start
{
  "targetEndpoint": "http://localhost:8091/payments",
  "requestRate": 50,
  "durationSeconds": 120
}
```

Monitor metrics in real-time:
```bash
watch -n 1 'curl -s http://localhost:8091/actuator/prometheus | grep mock_'
```

---

## 📁 Project Structure

```
backend-service/
├── src/main/java/com/nexusgate/backend_service/
│   ├── BackendServiceApplication.java          # Main application
│   ├── controller/
│   │   ├── UserController.java                 # User service
│   │   ├── OrderController.java                # Order service
│   │   └── PaymentController.java              # Payment service
│   └── dto/
│       ├── User.java
│       ├── CreateUserRequest.java
│       ├── Order.java
│       ├── CreateOrderRequest.java
│       ├── Payment.java
│       └── ProcessPaymentRequest.java
├── src/main/resources/
│   └── application.yml                         # Configuration
├── pom.xml                                     # Dependencies
├── README.md                                   # Documentation
└── test-backend.sh                             # Test script
```

---

## 🎓 Learning Points

### For Interviews

1. **Microservices Patterns**
   - Service separation
   - In-memory storage
   - Stateless design

2. **Observability**
   - Custom Prometheus metrics
   - Health checks
   - Actuator endpoints

3. **Realistic Testing**
   - Simulated delays
   - Random failures
   - Predictable behavior

4. **Clean Code**
   - Clear naming
   - Single responsibility
   - Well-documented

---

## ✨ What Makes This Special

1. **Three services in one** - Simple to run, easy to demonstrate
2. **Prometheus-ready** - Metrics from day one
3. **No external dependencies** - Just Java and Spring Boot
4. **Interview-friendly** - Clean, explainable, professional
5. **Production patterns** - Health checks, metrics, proper logging

---

## 🔮 Next Steps

To complete the monitoring stack:

1. **Add Prometheus** to docker-compose
2. **Add Grafana** to docker-compose
3. **Create dashboards** for all three services
4. **Test with Gateway** - Route through NexusGate
5. **Load test** - Validate rate limiting

---

## 📊 Expected Metrics After Load Test

After running 1000 requests through each service:

```
# Users created
mock_users_total{service="user-service"} 1000.0

# Orders created
mock_orders_total{service="order-service"} 1000.0

# Payments processed
mock_payments_success_total{service="payment-service",status="success"} ~900.0
mock_payments_failed_total{service="payment-service",status="failed"} ~100.0

# Request counts
http_server_requests_seconds_count{uri="/users",method="POST",status="201"} 1000
http_server_requests_seconds_count{uri="/orders",method="POST",status="201"} 1000
http_server_requests_seconds_count{uri="/payments",method="POST",status="201"} 1000
```

---

## 🎉 Summary

You now have:
- ✅ Three fully functional mock backend services
- ✅ Prometheus metrics with custom business counters
- ✅ HTTP request metrics (auto-generated)
- ✅ Health checks and actuator endpoints
- ✅ Realistic delays and failure simulation
- ✅ Complete documentation and test script
- ✅ Interview-ready codebase

**All services are simple, clean, and ready to integrate with Prometheus/Grafana!**
