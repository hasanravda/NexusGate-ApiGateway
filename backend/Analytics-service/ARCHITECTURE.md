# Analytics Service Architecture

## System Context

```
┌──────────────────────────────────────────────────────────────────┐
│                        NexusGate System                           │
│                                                                    │
│  ┌─────────┐                                                      │
│  │ Client  │                                                      │
│  └────┬────┘                                                      │
│       │                                                           │
│       ↓                                                           │
│  ┌─────────────────┐                                             │
│  │  API Gateway    │                                             │
│  │  (Port 8080)    │                                             │
│  └────┬────────┬───┘                                             │
│       │        │                                                  │
│       │        └──────────────────────┐                          │
│       ↓                                ↓                          │
│  ┌─────────────┐            ┌──────────────────┐                │
│  │Auth Service │            │Analytics Service │                │
│  │ (Port 8081) │            │   (Port 8085)    │                │
│  └─────────────┘            └────────┬─────────┘                │
│       ↓                               ↓                          │
│  ┌─────────────┐            ┌──────────────────┐                │
│  │ Backend     │            │   PostgreSQL     │                │
│  │ Services    │            │   + Prometheus   │                │
│  └─────────────┘            └──────────────────┘                │
└──────────────────────────────────────────────────────────────────┘
```

## Architecture Overview

The Analytics Service is designed with the following principles:

1. **Non-Blocking Event Reception**: Fire-and-forget model ensures Gateway never waits
2. **Dual Storage Strategy**: PostgreSQL for detailed logs, Prometheus for aggregated metrics
3. **Separation of Concerns**: Clear boundaries between event reception, persistence, and analytics
4. **Scheduled Aggregation**: Daily jobs for dashboard-friendly summaries

## Component Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                      Analytics Service                              │
│                                                                      │
│  ┌──────────────────┐         ┌──────────────────┐                │
│  │  LogController   │         │AnalyticsController│                │
│  │  POST /logs      │         │  GET /analytics/* │                │
│  │  (202 Accepted)  │         │  (Dashboard APIs) │                │
│  └────────┬─────────┘         └────────┬──────────┘                │
│           │                            │                            │
│           ↓                            ↓                            │
│  ┌────────────────────────────────────────────────┐                │
│  │              Service Layer                      │                │
│  │  ┌──────────────┐  ┌────────────────┐         │                │
│  │  │  LogService  │  │AnalyticsService│         │                │
│  │  │  - Process   │  │  - Overview    │         │                │
│  │  │  - Persist   │  │  - Recent Logs │         │                │
│  │  │  - Delegate  │  │  - Top Endpoints│        │                │
│  │  └──────┬───────┘  └────────┬───────┘         │                │
│  │         │                   │                  │                │
│  │         ↓                   ↓                  │                │
│  │  ┌─────────────┐   ┌───────────────┐          │                │
│  │  │MetricsService│   │AggregationSvc │          │                │
│  │  │- Counters   │   │- Scheduled    │          │                │
│  │  │- Timers     │   │- Daily Job    │          │                │
│  │  └──────┬──────┘   └───────┬───────┘          │                │
│  └─────────┼──────────────────┼──────────────────┘                │
│            │                  │                                    │
│            ↓                  ↓                                    │
│  ┌─────────────────┐  ┌──────────────────┐                        │
│  │   Micrometer    │  │  JPA Repositories│                        │
│  │   (In-Memory)   │  │  - RequestLog    │                        │
│  │                 │  │  - MetricsSummary│                        │
│  └────────┬────────┘  └────────┬─────────┘                        │
│           │                    │                                   │
└───────────┼────────────────────┼───────────────────────────────────┘
            │                    │
            ↓                    ↓
    ┌──────────────┐    ┌──────────────┐
    │  Prometheus  │    │  PostgreSQL  │
    │  (Scraper)   │    │  (Database)  │
    └──────────────┘    └──────────────┘
```

## Data Flow

### Event Reception Flow

```
1. Gateway sends POST /logs request
   └─→ LogController receives request
       └─→ Validates LogEventRequest (@Valid)
           └─→ Returns 202 Accepted immediately
               └─→ Delegates to LogService.processLogEvent()
                   ├─→ Parses timestamp
                   ├─→ Builds RequestLog entity
                   ├─→ Saves to PostgreSQL (via JPA)
                   ├─→ Updates Micrometer metrics
                   │   ├─→ nexus_requests_total++
                   │   ├─→ nexus_errors_total++ (if status >= 400)
                   │   ├─→ nexus_rate_limit_violations_total++ (if rateLimited)
                   │   └─→ nexus_request_latency.record(latencyMs)
                   └─→ Logs debug message
```

### Analytics Query Flow

```
1. Dashboard requests GET /analytics/overview
   └─→ AnalyticsController receives request
       └─→ Delegates to AnalyticsService.getOverview()
           ├─→ Calculate 24-hour window
           ├─→ Execute custom JPQL queries:
           │   ├─→ countRequestsBetween()
           │   ├─→ countErrorsBetween()
           │   ├─→ countRateLimitViolationsBetween()
           │   └─→ averageLatencyBetween()
           ├─→ Build AnalyticsOverview DTO
           └─→ Return to controller
               └─→ Return 200 OK with JSON
```

### Scheduled Aggregation Flow

```
Daily at 2:00 AM:
└─→ AggregationService.aggregateDailyMetrics()
    ├─→ Calculate yesterday's date range
    ├─→ Find all distinct serviceRouteIds
    ├─→ For each serviceRouteId:
    │   ├─→ Count total requests
    │   ├─→ Count errors (status >= 400)
    │   ├─→ Calculate average latency
    │   ├─→ Calculate P95 latency (approximate)
    │   ├─→ Build MetricsSummary entity
    │   └─→ Save to database
    └─→ Log completion
```

## Database Schema

### request_logs Table

```
┌─────────────────┬──────────────┬──────────────┐
│ Column          │ Type         │ Constraints  │
├─────────────────┼──────────────┼──────────────┤
│ id              │ BIGSERIAL    │ PRIMARY KEY  │
│ api_key_id      │ BIGINT       │              │
│ service_route_id│ BIGINT       │ NOT NULL     │
│ method          │ VARCHAR(10)  │ NOT NULL     │
│ path            │ VARCHAR(500) │ NOT NULL     │
│ status          │ INTEGER      │ NOT NULL     │
│ latency_ms      │ BIGINT       │ NOT NULL     │
│ client_ip       │ VARCHAR(50)  │              │
│ rate_limited    │ BOOLEAN      │ DEFAULT FALSE│
│ timestamp       │ TIMESTAMP    │ NOT NULL     │
└─────────────────┴──────────────┴──────────────┘

Indexes:
- idx_timestamp ON (timestamp)          -- Time-based queries
- idx_service_route ON (service_route_id) -- Service aggregations
- idx_api_key ON (api_key_id)           -- API key analysis
```

**Design Rationale**:
- `timestamp` indexed for range queries (last 24 hours, last 7 days)
- `service_route_id` indexed for service-level aggregations
- `api_key_id` indexed for per-API-key analytics
- `path` length limited to 500 chars for reasonable storage

### metrics_summary Table

```
┌─────────────────┬──────────────┬──────────────┐
│ Column          │ Type         │ Constraints  │
├─────────────────┼──────────────┼──────────────┤
│ id              │ BIGSERIAL    │ PRIMARY KEY  │
│ date            │ DATE         │ NOT NULL     │
│ service_route_id│ BIGINT       │ NOT NULL     │
│ total_requests  │ BIGINT       │ NOT NULL     │
│ error_count     │ BIGINT       │ NOT NULL     │
│ avg_latency_ms  │ DOUBLE       │ NOT NULL     │
│ p95_latency_ms  │ DOUBLE       │ NOT NULL     │
└─────────────────┴──────────────┴──────────────┘

Unique Constraint:
- UNIQUE(date, service_route_id)
```

**Design Rationale**:
- Pre-aggregated data for fast dashboard queries
- Daily granularity reduces storage growth
- Unique constraint prevents duplicate aggregations

## Metrics Strategy

### Why Prometheus for Rate Calculations?

**Prometheus is the source of truth for:**
- Request rates (requests/second)
- Error rates (errors/second)
- Latency percentiles (P50, P95, P99)
- Real-time monitoring and alerting

**PostgreSQL is the source of truth for:**
- Raw request logs
- Historical data analysis
- Dashboard queries (counts, top endpoints)
- Audit trails

### Metric Definitions

#### nexus_requests_total
```
Type: Counter
Labels: service_route_id
Purpose: Total requests processed by each service route
Increment: On every request
Query: rate(nexus_requests_total[5m])
```

#### nexus_errors_total
```
Type: Counter
Labels: service_route_id, status
Purpose: Total errors by service route and status code
Increment: When status >= 400
Query: rate(nexus_errors_total[5m]) / rate(nexus_requests_total[5m])
```

#### nexus_rate_limit_violations_total
```
Type: Counter
Labels: service_route_id
Purpose: Total rate-limited requests
Increment: When rateLimited == true
Query: rate(nexus_rate_limit_violations_total[5m])
```

#### nexus_request_latency
```
Type: Timer (Histogram + Summary)
Labels: service_route_id
Purpose: Request latency distribution
Record: latencyMs value on every request
Query: histogram_quantile(0.95, rate(nexus_request_latency_seconds_bucket[5m]))
```

## Error Handling

### Fire-and-Forget Model

The service MUST NOT propagate exceptions to the Gateway:

```java
@Transactional
public void processLogEvent(LogEventRequest request) {
    try {
        // Process event
        requestLogRepository.save(requestLog);
        metricsService.recordRequest(...);
    } catch (Exception e) {
        log.error("Failed to process log event: {}", e.getMessage(), e);
        // DO NOT THROW - analytics should not break Gateway
    }
}
```

**Rationale**: Analytics failures should never impact the Gateway's ability to serve requests.

### Validation Errors

Invalid requests return `400 Bad Request` immediately:

```java
@PostMapping
public ResponseEntity<?> receiveLog(@Valid @RequestBody LogEventRequest request) {
    // @Valid triggers automatic validation
    // If validation fails, Spring returns 400 with error details
    logService.processLogEvent(request);
    return ResponseEntity.accepted().build();
}
```

### Database Failures

Database connection failures are logged but not propagated:
- Metric updates continue (in-memory)
- Logs are lost (acceptable trade-off)
- Gateway is not impacted

## Performance Considerations

### Metric Caching

MetricsService uses ConcurrentHashMap to cache metric instances:

```java
private final ConcurrentMap<String, Counter> requestCounters = new ConcurrentHashMap<>();

public void recordRequest(Long serviceRouteId, ...) {
    String tag = String.valueOf(serviceRouteId);
    Counter counter = requestCounters.computeIfAbsent(tag, 
        t -> Counter.builder("nexus_requests_total")
                   .tag("service_route_id", t)
                   .register(meterRegistry));
    counter.increment();
}
```

**Benefits**:
- No repeated metric registration
- Thread-safe without locks
- O(1) metric lookup

### Database Optimization

**Indexes**: Three indexes on `request_logs`:
- `idx_timestamp`: Fast time-based queries
- `idx_service_route`: Fast service-level aggregations
- `idx_api_key`: Fast API key analysis

**Connection Pooling**: Configured for high throughput:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

### Transaction Management

LogService uses `@Transactional` for atomicity:
- Single transaction for log insert + metrics update
- Rollback on failure
- No partial state

## Scheduled Jobs

### Daily Aggregation Job

```java
@Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
public void aggregateDailyMetrics() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    Instant start = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant();
    Instant end = start.plus(Duration.ofDays(1));
    
    // Aggregate logs into metrics_summary
    List<Long> serviceRouteIds = requestLogRepository.findDistinctServiceRouteIds();
    for (Long serviceRouteId : serviceRouteIds) {
        // Calculate totals, errors, latencies
        // Save to metrics_summary
    }
}
```

**Benefits**:
- Pre-aggregated data for fast dashboard queries
- Reduced query complexity
- Historical trend analysis

## Security Considerations

### Input Validation

All inputs validated using Jakarta Bean Validation:

```java
@NotNull
@Min(100) @Max(599)
private Integer status;

@NotBlank
@Size(max = 500)
private String path;
```

### SQL Injection Prevention

Using JPA with JPQL prevents SQL injection:

```java
@Query("SELECT COUNT(r) FROM RequestLog r WHERE ...")
Long countRequestsBetween(@Param("start") Instant start, ...);
```

### No Authentication (Internal Service)

Analytics Service is internal-only:
- Not exposed to public internet
- Accessed only by Gateway
- No API key required

## Monitoring and Observability

### Health Checks

```bash
# Actuator health endpoint
GET /actuator/health

Response:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### Prometheus Metrics

```bash
# View all metrics
GET /actuator/prometheus

# Sample metrics
nexus_requests_total{service_route_id="2"} 1234.0
nexus_errors_total{service_route_id="2",status="500"} 45.0
nexus_rate_limit_violations_total{service_route_id="2"} 12.0
```

### Application Logs

```
2024-01-21 10:30:00 DEBUG [LogService] Processed log event: serviceRouteId=2, status=200, latencyMs=145
2024-01-21 02:00:00 INFO  [AggregationService] Daily aggregation completed: 15 services processed
```

## Deployment Architecture

### Docker Deployment

```yaml
services:
  analytics-service:
    build: ./Analytics-service
    ports:
      - "8085:8085"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/nexusgate
      - SPRING_DATASOURCE_USERNAME=nexususer
      - SPRING_DATASOURCE_PASSWORD=nexuspass
    depends_on:
      - postgres
    networks:
      - nexusgate-network
```

### Kubernetes Deployment (Future)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: analytics-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: analytics-service
  template:
    metadata:
      labels:
        app: analytics-service
    spec:
      containers:
      - name: analytics-service
        image: nexusgate/analytics-service:latest
        ports:
        - containerPort: 8085
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: url
```

## Future Enhancements

### 1. Real-time Dashboards

```
WebSocket endpoint for live updates:
GET /ws/analytics

Push events:
- New request received
- Error threshold exceeded
- Rate limit violation spike
```

### 2. Advanced Analytics

```
Machine learning integration:
- Anomaly detection (unusual traffic patterns)
- Predictive scaling (forecast resource needs)
- Fraud detection (suspicious client IPs)
```

### 3. Multi-tenancy

```
Partition logs by tenant:
- Separate schemas per tenant
- Isolated metrics per tenant
- Per-tenant quotas and limits
```

### 4. Data Retention Policies

```
Automatic archival:
- Move logs > 90 days to cold storage
- Purge logs > 365 days
- Keep aggregated summaries indefinitely
```

### 5. Distributed Tracing

```
Integration with Zipkin/Jaeger:
- Trace requests across services
- Visualize request flow
- Identify bottlenecks
```

## Design Decisions

### Why Fire-and-Forget?

**Decision**: Gateway sends analytics events without waiting for response

**Rationale**:
- Gateway latency must be minimal
- Analytics failures should not impact Gateway
- Eventual consistency is acceptable

**Trade-offs**:
- Potential for event loss on Analytics Service crash
- No immediate feedback on analytics processing

### Why Dual Storage (PostgreSQL + Prometheus)?

**Decision**: Store logs in PostgreSQL, metrics in Prometheus

**Rationale**:
- Prometheus excels at time-series metrics and alerting
- PostgreSQL excels at complex queries and joins
- Combining both provides best of both worlds

**Trade-offs**:
- Increased infrastructure complexity
- Potential for data inconsistency

### Why Scheduled Aggregation?

**Decision**: Daily aggregation job instead of real-time aggregation

**Rationale**:
- Reduces query complexity for dashboards
- Pre-computed metrics load faster
- Batch processing is more efficient

**Trade-offs**:
- Aggregated data is delayed (up to 24 hours)
- Additional storage for summaries

## Conclusion

The Analytics Service is a critical component of NexusGate that provides observability without impacting performance. Its fire-and-forget architecture, dual storage strategy, and comprehensive metrics ensure reliable analytics collection and rich dashboarding capabilities.
