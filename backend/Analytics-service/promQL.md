# Prometheus Queries for Analytics Service Dashboard

This document contains all PromQL queries for monitoring the Analytics Service (port 8085) using Prometheus and Grafana.

## Service Information
- **Service Name**: analytics-service
- **Port**: 8085
- **Prometheus Job**: analytics-service
- **Metrics Endpoint**: http://localhost:8085/actuator/prometheus

---

## Dashboard Metrics Queries

### 1. Total HTTP Requests
Total number of HTTP requests handled by the service.

```promql
sum(http_server_requests_seconds_count{job="analytics-service"})
```

---

### 2. Total Error Requests (4xx + 5xx)
Total number of HTTP requests that resulted in client or server errors.

```promql
sum(http_server_requests_seconds_count{job="analytics-service",status=~"[45].*"})
```

---

### 3. Error Rate Percentage
Percentage of requests that resulted in errors over the last 5 minutes.

```promql
100 * (
  sum(rate(http_server_requests_seconds_count{job="analytics-service",status=~"[45].*"}[5m]))
  /
  sum(rate(http_server_requests_seconds_count{job="analytics-service"}[5m]))
)
```

---

### 4. Average Response Time (milliseconds)
Average HTTP response time in milliseconds over the last 5 minutes.

```promql
1000 * (
  sum(rate(http_server_requests_seconds_sum{job="analytics-service"}[5m]))
  /
  sum(rate(http_server_requests_seconds_count{job="analytics-service"}[5m]))
)
```

---

### 5. Requests Per Second (Traffic Rate)
Current request rate per second over the last 5 minutes.

```promql
sum(rate(http_server_requests_seconds_count{job="analytics-service"}[5m]))
```

---

### 6. Top 5 Endpoints by Request Count
Top 5 most frequently accessed endpoints.

```promql
topk(5, sum by(uri) (http_server_requests_seconds_count{job="analytics-service"}))
```

---

### 7. Top 5 Slowest Endpoints by Average Latency
Top 5 endpoints with the highest average response time.

```promql
topk(5, 
  sum by(uri) (rate(http_server_requests_seconds_sum{job="analytics-service"}[5m]))
  /
  sum by(uri) (rate(http_server_requests_seconds_count{job="analytics-service"}[5m]))
)
```

---

### 8. Rate-Limit Violations ⚠️ (Optional - Custom Metric)
Number of rate-limit violations per second.

```promql
sum(rate(rate_limit_violations_total{job="analytics-service"}[5m]))
```

**⚠️ Note:** This is a custom metric that must be manually instrumented in your application code using Micrometer's Counter. It does not exist by default in Spring Boot Actuator metrics.

**Example Implementation:**
```java
@Autowired
private MeterRegistry meterRegistry;

// Increment when rate limit is violated
meterRegistry.counter("rate_limit_violations_total").increment();
```

---

### 9. JVM Heap Memory Usage (bytes)
Current heap memory usage in bytes.

```promql
sum(jvm_memory_used_bytes{job="analytics-service",area="heap"})
```

**Human-readable version (MB):**
```promql
sum(jvm_memory_used_bytes{job="analytics-service",area="heap"}) / 1024 / 1024
```

---

### 10. Process CPU Usage (percentage)
Current CPU usage of the Analytics service process.

```promql
process_cpu_usage{job="analytics-service"} * 100
```

---

### 11. Service Up/Down Health Status
Service availability status (1 = up, 0 = down).

```promql
up{job="analytics-service"}
```

---

## Additional Useful Queries

### JVM Memory by Type
```promql
sum by(id) (jvm_memory_used_bytes{job="analytics-service",area="heap"})
```

### Garbage Collection Time
```promql
rate(jvm_gc_pause_seconds_sum{job="analytics-service"}[5m])
```

### Thread Count
```promql
jvm_threads_live_threads{job="analytics-service"}
```

### System CPU Usage
```promql
system_cpu_usage{job="analytics-service"} * 100
```

### Requests by HTTP Method
```promql
sum by(method) (http_server_requests_seconds_count{job="analytics-service"})
```

### Requests by Status Code
```promql
sum by(status) (http_server_requests_seconds_count{job="analytics-service"})
```

### 95th Percentile Response Time
```promql
histogram_quantile(0.95, 
  sum by(le) (rate(http_server_requests_seconds_bucket{job="analytics-service"}[5m]))
)
```

---

## Query Tips

### Filtering Actuator Endpoints
To exclude Prometheus scrape requests from metrics:
```promql
http_server_requests_seconds_count{job="analytics-service",uri!="/actuator/prometheus"}
```

### Time Range Options
- `[1m]` - Last 1 minute
- `[5m]` - Last 5 minutes (recommended default)
- `[15m]` - Last 15 minutes
- `[1h]` - Last hour

### Alternative Label Usage
If your setup uses `exported_application` instead of `job`:
```promql
http_server_requests_seconds_count{exported_application="analytics-service"}
```

---

## Grafana Dashboard Import

For pre-built JVM dashboards, import these dashboard IDs in Grafana:
- **11955** - JVM (Micrometer)
- **4701** - JVM (Micrometer) Basic
- **12900** - Spring Boot 2.1 Statistics

---

## Testing Queries

To test these queries:
1. Open Prometheus UI: http://localhost:9090
2. Go to **Graph** tab
3. Paste any query above
4. Click **Execute**
5. View results in **Table** or **Graph** view

---

**Last Updated**: January 22, 2026  
**Service Version**: 1.0.0
