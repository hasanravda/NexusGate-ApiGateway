package com.nexusgate.gateway.filter;


import com.nexusgate.gateway.client.RateLimitClient;
import com.nexusgate.gateway.dto.ServiceRouteResponse;
import com.nexusgate.gateway.redis.RedisRateLimiterService;
import com.nexusgate.gateway.util.ErrorResponseUtil;
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
