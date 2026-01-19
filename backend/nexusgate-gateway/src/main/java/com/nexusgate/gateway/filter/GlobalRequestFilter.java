package com.nexusgate.gateway.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusgate.gateway.client.ServiceRouteClient;
import com.nexusgate.gateway.dto.ServiceRouteResponse;
import com.nexusgate.gateway.util.HeaderUtil;
import com.nexusgate.gateway.util.PathMatcherUtil;
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
public class GlobalRequestFilter implements GlobalFilter, Ordered {

    private final ServiceRouteClient serviceRouteClient;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String requestPath = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        String clientIp = HeaderUtil.getClientIp(exchange.getRequest());

        log.info("Incoming request path: {} {} from {}", method, requestPath, clientIp);

        // Fetch all active routes and find first matching route using wildcard pattern matching
        return serviceRouteClient.getAllActiveRoutes()
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
                .switchIfEmpty(Mono.defer(() -> {
                    // No route found - write error and return empty to stop processing
                    log.warn("No matching route found for path: {}", requestPath);
                    return writeErrorResponse(exchange, HttpStatus.NOT_FOUND, "Service route not found")
                            .then(Mono.empty());
                }))
                .flatMap(route -> {
                    log.info("Matched route - RouteId: {}, PublicPath: {}, TargetUrl: {}", 
                            route.getId(), route.getPublicPath(), route.getTargetUrl());
                    
                    exchange.getAttributes().put("serviceRoute", route);
                    exchange.getAttributes().put("publicPath", route.getPublicPath());
                    exchange.getAttributes().put("startTime", startTime);
                    return chain.filter(exchange);
                })
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    ServiceRouteResponse route = exchange.getAttribute("serviceRoute");
                    Long apiKeyId = exchange.getAttribute("apiKeyId");

                    log.info("Request completed: {} {} - Status: {} - Duration: {}ms - ApiKeyId: {} - RouteId: {}",
                            method, requestPath,
                            exchange.getResponse().getStatusCode(),
                            duration,
                            apiKeyId,
                            route != null ? route.getId() : null);
                });
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        // Check if response is already committed
        if (exchange.getResponse().isCommitted()) {
            log.warn("Response already committed, cannot write error response");
            return Mono.empty();
        }

        // Set status code only - do NOT modify headers in WebFlux after response starts
        exchange.getResponse().setStatusCode(status);

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
        return -100;
    }
}