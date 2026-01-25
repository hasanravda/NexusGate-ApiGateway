package com.nexusgate.gateway.filter;


import com.nexusgate.gateway.client.AnalyticsClient;
import com.nexusgate.gateway.client.RateLimitClient;
import com.nexusgate.gateway.dto.ServiceRouteResponse;
import com.nexusgate.gateway.redis.RedisRateLimiterService;
import com.nexusgate.gateway.util.ErrorResponseUtil;
import com.nexusgate.gateway.util.HeaderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimitClient rateLimitClient;
    private final RedisRateLimiterService redisRateLimiterService;
    private final ErrorResponseUtil errorResponseUtil;
    private final AnalyticsClient analyticsClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServiceRouteResponse route = exchange.getAttribute("serviceRoute");

        if (route == null || route.getRateLimitEnabled() == null || !route.getRateLimitEnabled()) {
            return chain.filter(exchange);
        }

        Long apiKeyId = exchange.getAttribute("apiKeyId");
        if (apiKeyId == null) {
            log.warn("Rate limiting enabled but no API key found for route: {}", route.getId());
            return chain.filter(exchange);
        }

        log.debug("Checking rate limits for ApiKeyId: {}, RouteId: {}", apiKeyId, route.getId());

        return rateLimitClient.checkRateLimit(apiKeyId, route.getId())
                .flatMap(rateLimitResponse -> {
                    if (rateLimitResponse.getIsActive() == null || !rateLimitResponse.getIsActive()) {
                        log.debug("Rate limiting not active for ApiKeyId: {}, RouteId: {}", apiKeyId, route.getId());
                        return chain.filter(exchange);
                    }

                    return redisRateLimiterService.isAllowed(
                            apiKeyId,
                            route.getId(),
                            rateLimitResponse.getRequestsPerMinute(),
                            rateLimitResponse.getRequestsPerHour()
                    ).flatMap(allowed -> {
                        if (!allowed) {
                            log.warn("Rate limit exceeded - ApiKeyId: {}, RouteId: {}, Path: {}", 
                                    apiKeyId, route.getId(), exchange.getRequest().getPath().value());
                            
                            // Mark as rate limited for analytics
                            exchange.getAttributes().put("rateLimited", true);
                            
                            // Send rate limit violation to Analytics Service
                            try {
                                String apiKeyValue = exchange.getAttribute("apiKeyValue");
                                String clientIp = HeaderUtil.getClientIp(exchange.getRequest());
                                String limitValue = String.format("%d/min, %d/hour", 
                                        rateLimitResponse.getRequestsPerMinute(),
                                        rateLimitResponse.getRequestsPerHour());
                                
                                analyticsClient.sendRateLimitViolation(
                                        apiKeyValue != null ? apiKeyValue : "unknown",
                                        route.getPublicPath(), // Use publicPath as service identifier
                                        exchange.getRequest().getPath().value(),
                                        exchange.getRequest().getMethod().name(),
                                        limitValue,
                                        0L, // actual value not tracked in this implementation
                                        clientIp
                                );
                            } catch (Exception e) {
                                log.warn("Failed to send rate limit violation: {}", e.getMessage());
                            }
                            
                            return errorResponseUtil.writeErrorResponse(exchange, HttpStatus.TOO_MANY_REQUESTS,
                                    "Rate limit exceeded");
                        }
                        log.debug("Rate limit check passed for ApiKeyId: {}, RouteId: {}", apiKeyId, route.getId());
                        return chain.filter(exchange);
                    });
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -80;
    }
}
