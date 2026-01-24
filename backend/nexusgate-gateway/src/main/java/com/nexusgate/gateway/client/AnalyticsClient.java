package com.nexusgate.gateway.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * AnalyticsClient
 * 
 * Non-blocking client for sending analytics events to Analytics Service
 * Uses fire-and-forget pattern (doesn't wait for response)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${analytics.service.url:http://localhost:8085}")
    private String analyticsServiceUrl;

    /**
     * Sends request log to Analytics Service (fire-and-forget)
     */
    public void sendRequestLog(Long apiKeyId, Long serviceRouteId, String method, 
                                String path, Integer status, Long latencyMs, 
                                String clientIp, Boolean rateLimited, Boolean blocked) {
        
        Map<String, Object> logRequest = new HashMap<>();
        logRequest.put("apiKeyId", apiKeyId != null ? apiKeyId : 0L);
        logRequest.put("serviceRouteId", serviceRouteId != null ? serviceRouteId : 0L);
        logRequest.put("method", method);
        logRequest.put("path", path);
        logRequest.put("status", status);
        logRequest.put("latencyMs", latencyMs);
        logRequest.put("clientIp", clientIp != null ? clientIp : "unknown");
        logRequest.put("rateLimited", rateLimited != null ? rateLimited : false);
        logRequest.put("blocked", blocked != null ? blocked : false);
        logRequest.put("timestamp", Instant.now().toString());

        // Fire-and-forget: send async without blocking
        webClientBuilder.build()
                .post()
                .uri(analyticsServiceUrl + "/analytics/logs/request")
                .bodyValue(logRequest)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> log.debug("Analytics log sent successfully for {} {}", method, path),
                        error -> log.warn("Failed to send analytics log for {} {} - {}", method, path, error.getMessage())
                );
    }

    /**
     * Sends rate limit violation to Analytics Service (fire-and-forget)
     */
    public void sendRateLimitViolation(String apiKey, String serviceName, String endpoint,
                                        String httpMethod, String limitValue, Long actualValue,
                                        String clientIp) {
        
        Map<String, Object> violationRequest = new HashMap<>();
        violationRequest.put("apiKey", apiKey);
        violationRequest.put("serviceName", serviceName);
        violationRequest.put("endpoint", endpoint);
        violationRequest.put("httpMethod", httpMethod);
        violationRequest.put("limitValue", limitValue);
        violationRequest.put("actualValue", actualValue);
        violationRequest.put("clientIp", clientIp);
        violationRequest.put("timestamp", Instant.now().toString());

        // Fire-and-forget: send async without blocking
        webClientBuilder.build()
                .post()
                .uri(analyticsServiceUrl + "/analytics/logs/violation")
                .bodyValue(violationRequest)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> log.debug("Rate limit violation sent to analytics for {}", endpoint),
                        error -> log.warn("Failed to send rate limit violation - {}", error.getMessage())
                );
    }
}
