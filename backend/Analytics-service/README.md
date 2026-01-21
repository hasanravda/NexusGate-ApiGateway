# Analytics Service

**Port:** 8085

## Overview

The Analytics Service is a **non-blocking event receiver** that collects analytics data from the NexusGate API Gateway. It operates in a **fire-and-forget** model, ensuring the Gateway never blocks waiting for analytics processing.

## Architecture

```
Gateway → POST /logs (async) → Analytics Service
                                      ↓
                        ┌─────────────┴──────────────┐
                        ↓                            ↓
                 PostgreSQL (logs)          Micrometer (metrics)
                        ↓                            ↓
                Dashboard APIs              Prometheus Scraper
```

### Key Design Principles

1. **Fire-and-Forget**: Gateway sends analytics events without waiting for response (202 Accepted)
2. **Non-Blocking**: Analytics processing never blocks the Gateway request path
3. **Dual Storage**: PostgreSQL for detailed logs, Prometheus for aggregated metrics
4. **Scheduled Aggregation**: Daily job aggregates logs into summaries for dashboard queries

## Components

### 1. Controllers

#### LogController
- **Endpoint**: `POST /logs`
- **Response**: `202 Accepted`
- **Purpose**: Receives analytics events from Gateway (fire-and-forget)

#### AnalyticsController
- **Endpoints**:
  - `GET /analytics/overview` - 24-hour overview
  - `GET /analytics/recent-requests?page=0&size=20` - Recent requests with pagination
  - `GET /analytics/top-endpoints?limit=10` - Top endpoints by request count

### 2. Services

#### LogService
- Persists request logs to PostgreSQL
- Updates Prometheus metrics via MetricsService
- Transactional processing

#### MetricsService
- Updates Micrometer counters and timers
- Maintains thread-safe metric caches
- Exposes metrics at `/actuator/prometheus`

#### AnalyticsService
- Read-only queries for dashboard data
- Calculates overview statistics
- Provides pagination for recent requests

#### AggregationService
- Scheduled daily job (2 AM)
- Aggregates previous day's logs
- Calculates P95 latency (approximate)
- Stores summaries in `metrics_summary` table

### 3. Entities

#### RequestLog
```java
id              BIGINT PRIMARY KEY
apiKeyId        BIGINT
serviceRouteId  BIGINT
method          VARCHAR(10)
path            VARCHAR(500)
status          INTEGER
latencyMs       BIGINT
clientIp        VARCHAR(50)
rateLimited     BOOLEAN
timestamp       TIMESTAMP
```

**Indexes**:
- `idx_timestamp` - For time-based queries
- `idx_service_route` - For service-level aggregations
- `idx_api_key` - For API key analysis

#### MetricsSummary
```java
id              BIGINT PRIMARY KEY
date            DATE
serviceRouteId  BIGINT
totalRequests   BIGINT
errorCount      BIGINT
avgLatencyMs    DOUBLE
p95LatencyMs    DOUBLE
```

## Prometheus Metrics

The service exposes the following custom metrics:

### 1. nexus_requests_total
- **Type**: Counter
- **Labels**: `service_route_id`
- **Description**: Total requests processed by each service route

### 2. nexus_errors_total
- **Type**: Counter
- **Labels**: `service_route_id`, `status`
- **Description**: Total errors (status >= 400) by service route and status code

### 3. nexus_rate_limit_violations_total
- **Type**: Counter
- **Labels**: `service_route_id`
- **Description**: Total rate-limited requests by service route

### 4. nexus_request_latency
- **Type**: Timer (Histogram + Summary)
- **Labels**: `service_route_id`
- **Description**: Request latency distribution by service route
- **Percentiles**: P50, P95, P99

## Event Contract

Gateway sends analytics events with the following structure:

```json
{
  "apiKeyId": 1,
  "serviceRouteId": 2,
  "method": "GET",
  "path": "/api/users/123",
  "status": 200,
  "latencyMs": 145,
  "rateLimited": false,
  "clientIp": "192.168.1.100",
  "timestamp": "2024-01-21T10:30:00.000Z"
}
```

### Validation Rules
- `method`: Required, enum [GET, POST, PUT, DELETE, PATCH]
- `path`: Required, max 500 chars
- `status`: Required, range [100-599]
- `latencyMs`: Required, min 0
- `rateLimited`: Required, boolean
- `timestamp`: Required, ISO-8601 format

## Database Setup

The service requires PostgreSQL with the following schema:

```sql
CREATE TABLE request_logs (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT,
    service_route_id BIGINT NOT NULL,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(500) NOT NULL,
    status INTEGER NOT NULL,
    latency_ms BIGINT NOT NULL,
    client_ip VARCHAR(50),
    rate_limited BOOLEAN NOT NULL DEFAULT FALSE,
    timestamp TIMESTAMP NOT NULL
);

CREATE INDEX idx_timestamp ON request_logs(timestamp);
CREATE INDEX idx_service_route ON request_logs(service_route_id);
CREATE INDEX idx_api_key ON request_logs(api_key_id);

CREATE TABLE metrics_summary (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    service_route_id BIGINT NOT NULL,
    total_requests BIGINT NOT NULL,
    error_count BIGINT NOT NULL,
    avg_latency_ms DOUBLE PRECISION NOT NULL,
    p95_latency_ms DOUBLE PRECISION NOT NULL,
    UNIQUE(date, service_route_id)
);
```

## Configuration

### application.yml

```yaml
server:
  port: 8085

spring:
  application:
    name: analytics-service
  datasource:
    url: jdbc:postgresql://localhost:5432/nexusgate
    username: nexususer
    password: nexuspass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  prometheus:
    metrics:
      export:
        enabled: true
```

## Running the Service

### Prerequisites
- Java 21
- PostgreSQL 15+
- Docker (optional)

### Using Maven

```bash
cd backend/Analytics-service

# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Or run the JAR
java -jar target/Analytics-service-0.0.1-SNAPSHOT.jar
```

### Using Docker (via infrastructure)

```bash
cd backend/infrastructure
docker-compose up analytics-service
```

## Testing

### Health Check
```bash
curl http://localhost:8085/actuator/health
```

### Send Test Log Event
```bash
curl -X POST http://localhost:8085/logs \
  -H "Content-Type: application/json" \
  -d '{
    "apiKeyId": 1,
    "serviceRouteId": 2,
    "method": "GET",
    "path": "/api/users/123",
    "status": 200,
    "latencyMs": 145,
    "rateLimited": false,
    "clientIp": "192.168.1.100",
    "timestamp": "2024-01-21T10:30:00.000Z"
  }'
```

Expected response:
```json
{
  "message": "Log event accepted"
}
```

### Check Prometheus Metrics
```bash
curl http://localhost:8085/actuator/prometheus | grep nexus_
```

### Get Analytics Overview
```bash
curl http://localhost:8085/analytics/overview
```

### Get Recent Requests
```bash
curl "http://localhost:8085/analytics/recent-requests?page=0&size=20"
```

### Get Top Endpoints
```bash
curl "http://localhost:8085/analytics/top-endpoints?limit=10"
```

## Scheduled Jobs

### Daily Aggregation Job
- **Schedule**: Daily at 2:00 AM
- **Purpose**: Aggregate previous day's logs into `metrics_summary` table
- **Process**:
  1. Query all logs from previous day
  2. Group by service_route_id and date
  3. Calculate: total_requests, error_count, avg_latency_ms, p95_latency_ms
  4. Insert/update metrics_summary table

## Monitoring

### Key Metrics to Monitor

1. **Log Processing Rate**: `nexus_requests_total` rate
2. **Error Rate**: `nexus_errors_total` / `nexus_requests_total`
3. **Rate Limit Violations**: `nexus_rate_limit_violations_total` rate
4. **Latency Distribution**: `nexus_request_latency` percentiles
5. **Database Connection Pool**: Spring Boot JPA metrics
6. **JVM Metrics**: Memory, GC, threads

### Prometheus Queries

```promql
# Request rate per service
rate(nexus_requests_total[5m])

# Error rate per service
rate(nexus_errors_total[5m]) / rate(nexus_requests_total[5m])

# P95 latency
histogram_quantile(0.95, rate(nexus_request_latency_seconds_bucket[5m]))

# Rate limit violation rate
rate(nexus_rate_limit_violations_total[5m])
```

## Integration with Gateway

The Gateway should send analytics events using a **non-blocking HTTP client** (WebClient):

```java
@Async
public void sendAnalyticsEvent(AnalyticsEvent event) {
    webClient.post()
        .uri("http://analytics-service:8085/logs")
        .bodyValue(event)
        .retrieve()
        .toBodilessEntity()
        .subscribe(
            response -> log.debug("Analytics event sent"),
            error -> log.warn("Failed to send analytics event: {}", error.getMessage())
        );
}
```

**Important**: Never block on the analytics response - use reactive patterns or async processing.

## Performance Considerations

1. **Database Indexing**: Ensure indexes on `timestamp`, `service_route_id`, and `api_key_id`
2. **Connection Pooling**: Configure appropriate pool sizes for high throughput
3. **Batch Processing**: Consider batching inserts for extreme high load
4. **Metric Caching**: MetricsService uses ConcurrentHashMap to cache metrics
5. **Async Processing**: LogService processes events asynchronously

## Troubleshooting

### High Memory Usage
- Check metric cache size in MetricsService
- Review JPA cache configuration
- Monitor GC behavior

### Slow Queries
- Check database indexes
- Review query execution plans
- Consider partitioning request_logs table by date

### Missing Metrics
- Verify Prometheus scrape interval
- Check /actuator/prometheus endpoint
- Review MetricsService counter initialization

### Event Loss
- Check database connection pool exhaustion
- Review transaction timeout settings
- Monitor LogService error logs

## Future Enhancements

1. **Real-time Dashboards**: WebSocket support for live updates
2. **Advanced Analytics**: Machine learning for anomaly detection
3. **Multi-tenancy**: Separate metrics per tenant/organization
4. **Data Retention**: Automatic archival and purging of old logs
5. **Distributed Tracing**: Integration with Zipkin/Jaeger
6. **Alert Rules**: Automated alerting for SLA violations

## License

Part of the NexusGate API Gateway project.
