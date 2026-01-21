# Gateway Integration Guide

This guide explains how to integrate the Analytics Service with the NexusGate API Gateway.

## Overview

The Gateway sends analytics events to the Analytics Service after each request is completed. This integration follows a **fire-and-forget** pattern to ensure the Gateway never blocks waiting for analytics processing.

## Architecture

```
Client → Gateway → [Auth → Rate Limiter → Backend] → Response
                 ↓
           Analytics Event (async)
                 ↓
         Analytics Service → PostgreSQL + Prometheus
```

**Key Principle**: Analytics Service is **NOT** in the request path.

---

## Integration Steps

### Step 1: Add WebClient Dependency (Gateway)

Add WebFlux WebClient to `nexusgate-gateway/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**Note**: Gateway is already reactive (Spring Cloud Gateway uses WebFlux), so WebClient is available.

---

### Step 2: Create Analytics Event DTO (Gateway)

Create `AnalyticsEvent.java` in the Gateway:

```java
package com.nexusgate.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {
    private Long apiKeyId;
    private Long serviceRouteId;
    private String method;
    private String path;
    private Integer status;
    private Long latencyMs;
    private Boolean rateLimited;
    private String clientIp;
    private String timestamp;

    public static AnalyticsEvent from(
            Long apiKeyId,
            Long serviceRouteId,
            String method,
            String path,
            Integer status,
            Long latencyMs,
            Boolean rateLimited,
            String clientIp) {
        return AnalyticsEvent.builder()
                .apiKeyId(apiKeyId)
                .serviceRouteId(serviceRouteId)
                .method(method)
                .path(path)
                .status(status)
                .latencyMs(latencyMs)
                .rateLimited(rateLimited != null ? rateLimited : false)
                .clientIp(clientIp)
                .timestamp(Instant.now().toString())
                .build();
    }
}
```

---

### Step 3: Create Analytics Client (Gateway)

Create `AnalyticsClient.java`:

```java
package com.nexusgate.gateway.client;

import com.nexusgate.gateway.dto.AnalyticsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AnalyticsClient {

    private final WebClient webClient;

    public AnalyticsClient(
            @Value("${analytics.service.url:http://localhost:8085}") String analyticsUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(analyticsUrl)
                .build();
    }

    /**
     * Send analytics event (fire-and-forget)
     * 
     * IMPORTANT: This method does NOT block the Gateway.
     * We subscribe() without waiting for the response.
     */
    public void sendEvent(AnalyticsEvent event) {
        webClient.post()
                .uri("/logs")
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> 
                    log.debug("Analytics event sent: {} {}", event.getMethod(), event.getPath()))
                .doOnError(error -> 
                    log.warn("Failed to send analytics event: {}", error.getMessage()))
                .onErrorResume(e -> Mono.empty()) // Swallow errors
                .subscribe(); // Fire-and-forget
    }
}
```

**Key Points**:
- ✅ Uses WebClient (reactive, non-blocking)
- ✅ `.subscribe()` at the end (fire-and-forget)
- ✅ Error handling (log and continue)
- ✅ No blocking calls (`.block()` is NOT used)

---

### Step 4: Add Configuration (Gateway)

Add to `application.yml`:

```yaml
analytics:
  service:
    url: http://localhost:8085  # Change to analytics-service:8085 in Docker
```

---

### Step 5: Update Gateway Filter (Gateway)

Modify your custom filter or create a new one to send analytics events:

```java
package com.nexusgate.gateway.filter;

import com.nexusgate.gateway.client.AnalyticsClient;
import com.nexusgate.gateway.dto.AnalyticsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AnalyticsFilter implements GlobalFilter, Ordered {

    @Autowired
    private AnalyticsClient analyticsClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // Calculate latency
                    long latencyMs = System.currentTimeMillis() - startTime;
                    ServerHttpResponse response = exchange.getResponse();

                    // Extract data from exchange attributes (set by previous filters)
                    Long apiKeyId = exchange.getAttribute("apiKeyId");
                    Long serviceRouteId = exchange.getAttribute("serviceRouteId");
                    Boolean rateLimited = exchange.getAttribute("rateLimited");

                    // Build and send analytics event
                    AnalyticsEvent event = AnalyticsEvent.from(
                            apiKeyId,
                            serviceRouteId,
                            request.getMethod().name(),
                            request.getPath().value(),
                            response.getStatusCode() != null ? response.getStatusCode().value() : 500,
                            latencyMs,
                            rateLimited,
                            getClientIp(request)
                    );

                    // Send event (fire-and-forget)
                    analyticsClient.sendEvent(event);
                });
    }

    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddress() != null 
                    ? request.getRemoteAddress().getAddress().getHostAddress() 
                    : "unknown";
        }
        return ip;
    }

    @Override
    public int getOrder() {
        // Run last (after auth, rate limiting, routing)
        return Ordered.LOWEST_PRECEDENCE;
    }
}
```

**Key Points**:
- ✅ `doFinally()` ensures event is sent after response
- ✅ Extracts `apiKeyId`, `serviceRouteId`, `rateLimited` from exchange attributes
- ✅ Calculates latency
- ✅ Fire-and-forget (no blocking)
- ✅ Runs last (Order = LOWEST_PRECEDENCE)

---

### Step 6: Set Exchange Attributes (Gateway)

In your Auth Filter and Rate Limit Filter, set attributes for the Analytics Filter:

**Auth Filter**:
```java
// After successful authentication
exchange.getAttributes().put("apiKeyId", apiKey.getId());
exchange.getAttributes().put("serviceRouteId", route.getId());
```

**Rate Limit Filter**:
```java
// If rate limit exceeded
exchange.getAttributes().put("rateLimited", true);

// If rate limit OK
exchange.getAttributes().put("rateLimited", false);
```

---

## Testing the Integration

### Step 1: Start Services

```bash
# Terminal 1: PostgreSQL
cd backend
docker-compose up postgres

# Terminal 2: Analytics Service
cd backend/Analytics-service
./mvnw spring-boot:run

# Terminal 3: Gateway
cd backend/nexusgate-gateway
./mvnw spring-boot:run
```

### Step 2: Send Test Request

```bash
curl -X GET http://localhost:8080/api/users \
     -H "X-API-Key: your-api-key"
```

### Step 3: Verify Analytics Event

Check Analytics Service logs:
```
2024-01-21 10:30:00 DEBUG [LogService] Processed log event: serviceRouteId=2, status=200, latencyMs=145
```

Check Prometheus metrics:
```bash
curl http://localhost:8085/actuator/prometheus | grep nexus_requests_total
```

Check database:
```sql
SELECT * FROM request_logs ORDER BY timestamp DESC LIMIT 10;
```

---

## Troubleshooting

### No Events Received

1. **Check Gateway logs**: Look for "Failed to send analytics event"
2. **Check Analytics Service health**: `curl http://localhost:8085/actuator/health`
3. **Verify URL**: Ensure `analytics.service.url` is correct
4. **Check exchange attributes**: Log `apiKeyId`, `serviceRouteId` in AnalyticsFilter

### High Latency

1. **Check WebClient timeout**: Default is no timeout (good for fire-and-forget)
2. **Monitor network**: Analytics Service should respond with 202 in < 10ms
3. **Check database connection pool**: Analytics Service should have sufficient connections

### Missing Data

1. **Check validation errors**: Analytics Service returns 400 for invalid requests
2. **Check database constraints**: Ensure all required fields are set
3. **Check timestamp format**: Must be ISO-8601 (e.g., "2024-01-21T10:30:00.000Z")

---

## Docker Compose Integration

Add Analytics Service to `backend/docker-compose.yml`:

```yaml
services:
  analytics-service:
    build:
      context: ../Analytics-service
      dockerfile: Dockerfile
    container_name: analytics-service
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

  nexusgate-gateway:
    # ... existing config ...
    environment:
      - ANALYTICS_SERVICE_URL=http://analytics-service:8085
    depends_on:
      - analytics-service
```

---

## Prometheus Integration

Add Analytics Service to `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'analytics-service'
    scrape_interval: 15s
    static_configs:
      - targets: ['analytics-service:8085']
    metrics_path: '/actuator/prometheus'
```

---

## Grafana Dashboard

Create a dashboard with the following panels:

### 1. Request Rate
```promql
rate(nexus_requests_total[5m])
```

### 2. Error Rate
```promql
rate(nexus_errors_total[5m]) / rate(nexus_requests_total[5m]) * 100
```

### 3. P95 Latency
```promql
histogram_quantile(0.95, rate(nexus_request_latency_seconds_bucket[5m])) * 1000
```

### 4. Rate Limit Violations
```promql
rate(nexus_rate_limit_violations_total[5m])
```

---

## Performance Considerations

### Gateway Side

1. **Non-blocking**: Always use WebClient, never RestTemplate
2. **Fire-and-forget**: Use `.subscribe()`, never `.block()`
3. **Error handling**: Swallow errors (analytics should not break Gateway)
4. **Timeout**: Set reasonable timeout (optional, but recommended)

```java
this.webClient = WebClient.builder()
        .baseUrl(analyticsUrl)
        .defaultHeaders(headers -> {
            headers.setContentType(MediaType.APPLICATION_JSON);
        })
        .build();
```

### Analytics Service Side

1. **Fast response**: Return 202 immediately
2. **Async processing**: LogService processes asynchronously
3. **Database pooling**: Sufficient connection pool size
4. **Indexing**: Ensure indexes on `timestamp`, `service_route_id`, `api_key_id`

---

## Security Considerations

### Internal-Only Service

Analytics Service should NOT be exposed to the public internet:

```yaml
# Docker Compose
services:
  analytics-service:
    networks:
      - nexusgate-network  # Internal network only
    # Do NOT expose port 8085 to host in production
```

### No Authentication Required

Analytics Service trusts all requests from the Gateway:
- No API key required
- No OAuth2/JWT required
- Gateway is the only client

### Future Enhancement: Mutual TLS

For production, consider mutual TLS between Gateway and Analytics Service:

```java
SSLContext sslContext = SSLContextBuilder.create()
        .loadTrustMaterial(trustStore, password)
        .loadKeyMaterial(keyStore, password)
        .build();

this.webClient = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(
            HttpClient.create().secure(sslSpec -> 
                sslSpec.sslContext(sslContext))))
        .build();
```

---

## Monitoring

### Key Metrics to Monitor

1. **Gateway → Analytics Success Rate**
   - Log "Failed to send analytics event" warnings
   - Alert if failure rate > 5%

2. **Analytics Service Latency**
   - Should respond with 202 in < 10ms
   - Alert if P95 > 50ms

3. **Database Connection Pool**
   - Monitor active connections
   - Alert if pool exhaustion

4. **Prometheus Scrape Errors**
   - Monitor Prometheus scrape failures
   - Alert if scrape failure rate > 1%

---

## Summary

The integration between Gateway and Analytics Service follows these principles:

1. ✅ **Fire-and-Forget**: Gateway never blocks
2. ✅ **Non-Blocking**: WebClient with `.subscribe()`
3. ✅ **Error Handling**: Analytics failures don't impact Gateway
4. ✅ **Minimal Latency**: Analytics Service returns 202 immediately
5. ✅ **Rich Context**: apiKeyId, serviceRouteId, rateLimited passed via exchange attributes
6. ✅ **Internal-Only**: Analytics Service not exposed publicly
7. ✅ **Comprehensive Metrics**: Prometheus metrics for monitoring

The integration is production-ready and follows Spring Cloud Gateway best practices!
