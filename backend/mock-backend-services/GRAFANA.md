# Grafana Dashboard Configuration Guide

## 📊 Available Metrics for Visualization

This guide shows all metrics available from the backend-service that you can visualize in Grafana.

---

## 🎯 Business Metrics (Custom)

These are the custom metrics we created for tracking business operations:

### User Service Metrics
```promql
# Total users created over time
mock_users_total{service="user-service"}

# User creation rate (per second)
rate(mock_users_total[1m])

# User creation rate (per minute)
rate(mock_users_total[1m]) * 60
```

### Order Service Metrics
```promql
# Total orders created over time
mock_orders_total{service="order-service"}

# Order creation rate (per second)
rate(mock_orders_total[1m])

# Order creation rate (per minute)
rate(mock_orders_total[1m]) * 60
```

### Payment Service Metrics
```promql
# Total successful payments
mock_payments_success_total{service="payment-service"}

# Total failed payments
mock_payments_failed_total{service="payment-service"}

# Payment success rate (percentage)
rate(mock_payments_success_total[5m]) / 
(rate(mock_payments_success_total[5m]) + rate(mock_payments_failed_total[5m])) * 100

# Payment failure rate (percentage)
rate(mock_payments_failed_total[5m]) / 
(rate(mock_payments_success_total[5m]) + rate(mock_payments_failed_total[5m])) * 100

# Total payments processed (success + failed)
increase(mock_payments_success_total[5m]) + increase(mock_payments_failed_total[5m])
```

---

## 📈 HTTP Request Metrics (Auto-Generated)

Spring Boot Actuator automatically exposes these metrics:

### Request Rate
```promql
# Total requests per second (all endpoints)
rate(http_server_requests_seconds_count{application="backend-service"}[1m])

# Requests per second by endpoint
rate(http_server_requests_seconds_count{application="backend-service",uri="/users"}[1m])

# Requests per second by method
rate(http_server_requests_seconds_count{application="backend-service",method="POST"}[1m])

# Requests per second by status code
rate(http_server_requests_seconds_count{application="backend-service",status="200"}[1m])
```

### Latency Metrics
```promql
# Average response time (seconds)
rate(http_server_requests_seconds_sum{application="backend-service"}[5m]) /
rate(http_server_requests_seconds_count{application="backend-service"}[5m])

# Average response time in milliseconds
(rate(http_server_requests_seconds_sum{application="backend-service"}[5m]) /
rate(http_server_requests_seconds_count{application="backend-service"}[5m])) * 1000

# P95 latency (95th percentile)
histogram_quantile(0.95, 
  rate(http_server_requests_seconds_bucket{application="backend-service"}[5m])
)

# P99 latency (99th percentile)
histogram_quantile(0.99, 
  rate(http_server_requests_seconds_bucket{application="backend-service"}[5m])
)

# Maximum observed latency
http_server_requests_seconds_max{application="backend-service"}
```

### Error Rate
```promql
# Total errors (4xx + 5xx) per second
rate(http_server_requests_seconds_count{application="backend-service",status=~"4..|5.."}[1m])

# Error rate percentage
(rate(http_server_requests_seconds_count{application="backend-service",status=~"4..|5.."}[1m]) /
rate(http_server_requests_seconds_count{application="backend-service"}[1m])) * 100

# 404 errors specifically
rate(http_server_requests_seconds_count{application="backend-service",status="404"}[1m])
```

---

## 💻 JVM Metrics

### Memory Usage
```promql
# Heap memory used (bytes)
jvm_memory_used_bytes{application="backend-service",area="heap"}

# Heap memory used (MB)
jvm_memory_used_bytes{application="backend-service",area="heap"} / 1024 / 1024

# Heap memory committed (MB)
jvm_memory_committed_bytes{application="backend-service",area="heap"} / 1024 / 1024

# Memory usage percentage
(jvm_memory_used_bytes{application="backend-service",area="heap"} /
jvm_memory_max_bytes{application="backend-service",area="heap"}) * 100
```

### Garbage Collection
```promql
# GC pause time (seconds)
rate(jvm_gc_pause_seconds_sum[1m])

# GC pause count
rate(jvm_gc_pause_seconds_count[1m])

# Average GC pause duration
rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m])
```

### Thread Metrics
```promql
# Active threads
jvm_threads_states_threads{application="backend-service",state="runnable"}

# Total threads
jvm_threads_live_threads{application="backend-service"}

# Peak threads
jvm_threads_peak_threads{application="backend-service"}
```

---

## 🖥️ System Metrics

### CPU Usage
```promql
# Process CPU usage (0-1, where 1 = 100%)
process_cpu_usage{application="backend-service"}

# Process CPU usage percentage
process_cpu_usage{application="backend-service"} * 100

# System CPU usage
system_cpu_usage{application="backend-service"}

# CPU count
system_cpu_count{application="backend-service"}
```

### Process Info
```promql
# Process uptime (seconds)
process_uptime_seconds{application="backend-service"}

# Process uptime (hours)
process_uptime_seconds{application="backend-service"} / 3600

# Start time
process_start_time_seconds{application="backend-service"}
```

---

## 📱 Sample Grafana Dashboard Panels

### Panel 1: Request Rate Dashboard
```
Title: Total Requests Per Second
Query: rate(http_server_requests_seconds_count{application="backend-service"}[1m])
Type: Graph (Time Series)
Unit: req/s
```

### Panel 2: Latency Dashboard
```
Title: Response Time (P50, P95, P99)
Queries:
  - P50: histogram_quantile(0.50, rate(http_server_requests_seconds_bucket[5m]))
  - P95: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
  - P99: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
Type: Graph (Time Series)
Unit: seconds (s)
```

### Panel 3: Business Metrics Dashboard
```
Title: Operations Per Minute
Queries:
  - Users: rate(mock_users_total[1m]) * 60
  - Orders: rate(mock_orders_total[1m]) * 60
  - Payments: rate(mock_payments_success_total[1m]) * 60
Type: Graph (Time Series)
Unit: ops/min
Legend: {{service}}
```

### Panel 4: Payment Success Rate
```
Title: Payment Success Rate
Query: (rate(mock_payments_success_total[5m]) / 
       (rate(mock_payments_success_total[5m]) + rate(mock_payments_failed_total[5m]))) * 100
Type: Gauge
Unit: percent (0-100)
Thresholds: Red < 85, Yellow < 95, Green >= 95
```

### Panel 5: Memory Usage
```
Title: Heap Memory Usage
Query: jvm_memory_used_bytes{application="backend-service",area="heap"} / 1024 / 1024
Type: Graph (Time Series)
Unit: megabytes (MB)
```

### Panel 6: Error Rate
```
Title: Error Rate (%)
Query: (rate(http_server_requests_seconds_count{status=~"4..|5.."}[1m]) /
       rate(http_server_requests_seconds_count[1m])) * 100
Type: Stat
Unit: percent (0-100)
Color: Red if > 0
```

---

## 🎨 Complete Dashboard JSON Template

Here's a basic structure for a Grafana dashboard:

```json
{
  "dashboard": {
    "title": "Backend Services - Business Metrics",
    "panels": [
      {
        "title": "User Creation Rate",
        "targets": [{
          "expr": "rate(mock_users_total[1m]) * 60"
        }],
        "type": "graph"
      },
      {
        "title": "Order Creation Rate",
        "targets": [{
          "expr": "rate(mock_orders_total[1m]) * 60"
        }],
        "type": "graph"
      },
      {
        "title": "Payment Success Rate",
        "targets": [{
          "expr": "(rate(mock_payments_success_total[5m]) / (rate(mock_payments_success_total[5m]) + rate(mock_payments_failed_total[5m]))) * 100"
        }],
        "type": "gauge"
      },
      {
        "title": "Request Latency (P95)",
        "targets": [{
          "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) * 1000"
        }],
        "type": "graph"
      }
    ]
  }
}
```

---

## 🔍 Useful Queries for Troubleshooting

### Find Slowest Endpoints
```promql
topk(5, 
  rate(http_server_requests_seconds_sum{application="backend-service"}[5m]) /
  rate(http_server_requests_seconds_count{application="backend-service"}[5m])
)
```

### Find Most Called Endpoints
```promql
topk(5, rate(http_server_requests_seconds_count{application="backend-service"}[5m]))
```

### Memory Leak Detection
```promql
# If this keeps increasing, might indicate memory leak
deriv(jvm_memory_used_bytes{area="heap"}[10m])
```

### High Error Rate Alert
```promql
# Alert if error rate > 5%
(rate(http_server_requests_seconds_count{status=~"5.."}[5m]) /
rate(http_server_requests_seconds_count[5m])) * 100 > 5
```

---

## 📋 Recommended Dashboard Sections

### Section 1: Business KPIs
- User creation rate
- Order creation rate
- Payment success rate
- Payment failure rate

### Section 2: Performance
- Request rate (requests/sec)
- Average latency
- P95 latency
- P99 latency

### Section 3: Reliability
- Error rate (%)
- Success rate (%)
- Uptime
- Failed requests count

### Section 4: Resource Usage
- Heap memory usage
- CPU usage
- Thread count
- GC activity

### Section 5: Detailed Breakdown
- Requests by endpoint
- Requests by method
- Requests by status code
- Latency by endpoint

---

## 🚀 Quick Setup

1. **Start Prometheus** (scraping from `:8091/actuator/prometheus`)
2. **Start Grafana**
3. **Add Prometheus as data source** in Grafana
4. **Import dashboard** or create panels using queries above
5. **Generate load** using test script or load-tester-service
6. **Watch metrics update in real-time**

---

## 💡 Tips

- Use **5m** window for smooth graphs (not too noisy)
- Use **rate()** for per-second rates from counters
- Use **increase()** for total change over time window
- Use **histogram_quantile()** for percentile calculations
- Add **by (uri)** to group metrics by endpoint
- Use **topk()** to find top N values

---

## 🎯 Success Criteria

After setup, you should see:
- ✅ All 4 custom business metrics updating
- ✅ HTTP request metrics showing activity
- ✅ Payment success rate around 90%
- ✅ Latency matching expected delays (50-700ms)
- ✅ Zero errors under normal operation
- ✅ Memory usage stable
- ✅ CPU usage proportional to load

---

**You now have everything needed to create comprehensive Grafana dashboards for your mock backend services!**
