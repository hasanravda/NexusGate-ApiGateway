package com.nexusgate.gateway.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusgate.gateway.client.RateLimitClient;
import com.nexusgate.gateway.dto.ServiceRouteResponse;
import com.nexusgate.gateway.redis.RedisRateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimitClient rateLimitClient;
    private final RedisRateLimiterService redisRateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServiceRouteResponse route = exchange.getAttribute("serviceRoute");

        if (route == null || route.getRateLimitEnabled() == null || !route.getRateLimitEnabled()) {
            return chain.filter(exchange);
        }

        Long apiKeyId = exchange.getAttribute("apiKeyId");
        if (apiKeyId == null) {
            log.warn("Rate limiting enabled but no API key found");
            return chain.filter(exchange);
        }

        return rateLimitClient.checkRateLimit(apiKeyId, route.getId())
                .flatMap(rateLimitResponse -> {
                    if (rateLimitResponse.getIsActive() == null || !rateLimitResponse.getIsActive()) {
                        return chain.filter(exchange);
                    }

                    return redisRateLimiterService.isAllowed(
                            apiKeyId,
                            route.getId(),
                            rateLimitResponse.getRequestsPerMinute(),
                            rateLimitResponse.getRequestsPerHour()
                    ).flatMap(allowed -> {
                        if (!allowed) {
                            return writeErrorResponse(exchange, HttpStatus.TOO_MANY_REQUESTS,
                                    "Rate limit exceeded");
                        }
                        return chain.filter(exchange);
                    });
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("path", exchange.getRequest().getPath().value());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to write error response", e);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -80;
    }
}
