package com.nexusgate.gateway.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusgate.gateway.client.AnalyticsClient;
import com.nexusgate.gateway.client.ApiKeyClient;
import com.nexusgate.gateway.client.ServiceRouteClient;
import com.nexusgate.gateway.dto.ApiKeyResponse;
import com.nexusgate.gateway.dto.ServiceRouteResponse;
import com.nexusgate.gateway.exception.ApiKeyInvalidException;
import com.nexusgate.gateway.service.ApiKeyCacheService;
import com.nexusgate.gateway.service.RouteCacheService;
import com.nexusgate.gateway.util.ErrorResponseUtil;
import com.nexusgate.gateway.util.HeaderUtil;
import com.nexusgate.gateway.util.PathMatcherUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalRequestFilter implements GlobalFilter, Ordered {

    private final RouteCacheService routeCacheService;
    private final ApiKeyCacheService apiKeyCacheService;
    private final ErrorResponseUtil errorResponseUtil;
    private final AnalyticsClient analyticsClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String requestPath = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        String clientIp = HeaderUtil.getClientIp(exchange.getRequest());

        log.info("Incoming request path: {} {} from {}", method, requestPath, clientIp);

        // Use cached routes instead of calling config-service on every request
        return routeCacheService.getCachedRoutes()
                .filter(route -> {
                    // Check if route is active
                    if (route.getIsActive() == null || !route.getIsActive()) {
                        return false;
                    }
                    // Match using AntPathMatcher for wildcard support
                    boolean matches = PathMatcherUtil.matches(route.getPublicPath(), requestPath);
                    if (matches) {
                        log.debug("Route pattern '{}' matched request path '{}'", route.getPublicPath(), requestPath);
                    }
                    return matches;
                })
                .next() // Get first matching route
                .flatMap(route -> {
                    log.info("Matched route - RouteId: {}, PublicPath: {}, TargetUrl: {}, RequiresApiKey: {}",
                            route.getId(), route.getPublicPath(), route.getTargetUrl(), route.getRequiresApiKey());

                    // Check if route requires API key validation
                    Boolean requiresApiKey = route.getRequiresApiKey();
                    if (requiresApiKey == null) {
                        requiresApiKey = true; // Default to true for safety
                    }

                    // If route doesn't require API key, skip validation and proceed
                    if (!requiresApiKey) {
                        log.info("Route does not require API key - RouteId: {}, Path: {} - Skipping API key validation",
                                route.getId(), requestPath);
                        
                        // Store route info but no API key validation
                        exchange.getAttributes().put("serviceRoute", route);
                        exchange.getAttributes().put("publicPath", route.getPublicPath());
                        exchange.getAttributes().put("startTime", startTime);
                        
                        return chain.filter(exchange);
                    }

                    // Route requires API key - validate it
                    log.debug("Route requires API key - RouteId: {}, Path: {} - Validating API key",
                            route.getId(), requestPath);

                    // Extract API key from header
                    String apiKey = HeaderUtil.extractApiKey(exchange.getRequest());

                    // Validate API key is present and not blank
                    if (apiKey == null || apiKey.trim().isEmpty()) {
                        log.warn("API key missing for path: {} - Returning 401 UNAUTHORIZED", requestPath);
                        exchange.getAttributes().put("blocked", true);
                        return errorResponseUtil.writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "API key is missing");
                    }

                    log.debug("API key found for path: {}, validating from cache...", requestPath);

                    // Validate API key from cache (instant lookup, zero network calls!)
                    return apiKeyCacheService.validateApiKey(apiKey)
                            .flatMap(apiKeyResponse -> {
                                // Check if API key is active
                                if (apiKeyResponse.getIsActive() == null || !apiKeyResponse.getIsActive()) {
                                    log.warn("API key is inactive for path: {} - Returning 401 UNAUTHORIZED", requestPath);
                                    exchange.getAttributes().put("blocked", true);
                                    return errorResponseUtil.writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "API key is inactive");
                                }

                                // Check if API key is expired
                                if (apiKeyResponse.getExpiresAt() != null && 
                                    apiKeyResponse.getExpiresAt().isBefore(LocalDateTime.now())) {
                                    log.warn("API key is expired for path: {} - Returning 401 UNAUTHORIZED", requestPath);
                                    exchange.getAttributes().put("blocked", true);
                                    return errorResponseUtil.writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "API key is expired");
                                }

                                log.info("API key validation successful - ApiKeyId: {}", apiKeyResponse.getId());

                                // Store validated API key information in exchange attributes
                                exchange.getAttributes().put("apiKeyId", apiKeyResponse.getId());
                                exchange.getAttributes().put("apiKeyValue", apiKeyResponse.getKeyValue());
                                exchange.getAttributes().put("serviceRoute", route);
                                exchange.getAttributes().put("publicPath", route.getPublicPath());
                                exchange.getAttributes().put("startTime", startTime);
                                
                                return chain.filter(exchange);
                            })
                            .onErrorResume(ApiKeyInvalidException.class, e -> {
                                log.warn("Invalid API key for path: {} - {}", requestPath, e.getMessage());
                                exchange.getAttributes().put("blocked", true);
                                return errorResponseUtil.writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Invalid API key");
                            })
                            .onErrorResume(e -> {
                                log.error("Config service unavailable for path: {} - {}", requestPath, e.getMessage());
                                return errorResponseUtil.writeErrorResponse(exchange, HttpStatus.SERVICE_UNAVAILABLE, 
                                        "Authentication service temporarily unavailable");
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // No route found - write error response and stop processing
                    log.warn("No matching route found for path: {}", requestPath);
                    return errorResponseUtil.writeErrorResponse(exchange, HttpStatus.NOT_FOUND, "Service route not found");
                }))
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    ServiceRouteResponse route = exchange.getAttribute("serviceRoute");
                    Long apiKeyId = exchange.getAttribute("apiKeyId");
                    Boolean rateLimited = exchange.getAttribute("rateLimited");
                    Boolean blocked = exchange.getAttribute("blocked");
                    Integer statusCode = exchange.getResponse().getStatusCode() != null ? 
                            exchange.getResponse().getStatusCode().value() : 500;

                    log.info("Request completed: {} {} - Status: {} - Duration: {}ms - ApiKeyId: {} - RouteId: {}",
                            method, requestPath,
                            statusCode,
                            duration,
                            apiKeyId,
                            route != null ? route.getId() : null);

                    // Send analytics log to Analytics Service (fire-and-forget)
                    try {
                        analyticsClient.sendRequestLog(
                                apiKeyId,
                                route != null ? route.getId() : null,
                                method,
                                requestPath,
                                statusCode,
                                duration,
                                clientIp,
                                rateLimited != null ? rateLimited : false,
                                blocked != null ? blocked : (statusCode == 401 || statusCode == 403)
                        );
                    } catch (Exception e) {
                        log.warn("Failed to send analytics log: {}", e.getMessage());
                    }
                });
    }

    @Override
    public int getOrder() {
        return -100;
    }
}