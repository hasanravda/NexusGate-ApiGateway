package com.nexusgate.gateway.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusgate.gateway.dto.ServiceRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRoutingFilter implements GlobalFilter, Ordered {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServiceRouteResponse route = exchange.getAttribute("serviceRoute");
        if (route == null) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        Long apiKeyId = exchange.getAttribute("apiKeyId");

        // Build headers - copy from request but exclude hop-by-hop headers and sensitive headers
        HttpHeaders headers = new HttpHeaders();
        request.getHeaders().forEach((key, values) -> {
            // Skip hop-by-hop headers that shouldn't be forwarded
            String lowerKey = key.toLowerCase();
            if (!lowerKey.equals("host") && !lowerKey.equals("connection") && 
                !lowerKey.equals("keep-alive") && !lowerKey.equals("transfer-encoding") &&
                !lowerKey.equals("te") && !lowerKey.equals("trailer") && 
                !lowerKey.equals("proxy-authorization") && !lowerKey.equals("proxy-authenticate") &&
                !lowerKey.equals("upgrade") && !lowerKey.equals("x-api-key")) {  // Remove X-API-KEY header
                headers.addAll(key, values);
            }
        });

        if (route.getCustomHeaders() != null && !route.getCustomHeaders().isEmpty()) {
            try {
                Map<String, String> customHeaders = objectMapper.readValue(
                        route.getCustomHeaders(),
                        new TypeReference<Map<String, String>>() {}
                );
                customHeaders.forEach(headers::add);
            } catch (Exception e) {
                log.error("Failed to parse custom headers", e);
            }
        }

        // Inject internal headers for backend services
        if (apiKeyId != null) {
            headers.add("X-Api-Key-Id", String.valueOf(apiKeyId));
            log.debug("Injected X-Api-Key-Id: {} for route: {}", apiKeyId, route.getId());
        }
        headers.add("X-Route-Id", String.valueOf(route.getId()));
        log.debug("Injected X-Route-Id: {}", route.getId());

        int timeoutMs = route.getTimeoutMs() != null ? route.getTimeoutMs() : 30000;
        
        // Create new WebClient for each request to avoid connection reuse issues
        WebClient client = webClientBuilder
                .build();

        return forwardRequest(client, request, timeoutMs, route.getTargetUrl(), headers, exchange);
    }

    private Mono<Void> forwardRequest(WebClient client, ServerHttpRequest request,
                                      int timeoutMs, String targetUrl, HttpHeaders headers, 
                                      ServerWebExchange exchange) {
        HttpMethod method = request.getMethod();
        String fullPath = request.getPath().value();
        String publicPath = exchange.getAttribute("publicPath");
        Long apiKeyId = exchange.getAttribute("apiKeyId");
        ServiceRouteResponse route = exchange.getAttribute("serviceRoute");
        long startTime = System.currentTimeMillis();
        
        // Extract remaining path after the publicPath pattern
        String remainingPath = extractRemainingPath(fullPath, publicPath);
        
        // If remaining path is just "/" or empty, don't append anything
        if (remainingPath.isEmpty() || remainingPath.equals("/")) {
            remainingPath = "";
        }
        
        // Build complete URL
        String query = request.getURI().getRawQuery();
        final String completeUrl = query != null 
            ? targetUrl + remainingPath + "?" + query 
            : targetUrl + remainingPath;
        
        log.info("Forwarding request - Method: {}, Path: {}, TargetUrl: {}, RouteId: {}, ApiKeyId: {}", 
                method, fullPath, completeUrl, route != null ? route.getId() : null, apiKeyId);

        WebClient.RequestBodySpec requestBodySpec = client.method(method)
                .uri(completeUrl)
                .headers(h -> h.addAll(headers));

        // Only add body for methods that support request bodies (POST, PUT, PATCH, DELETE)
        WebClient.RequestHeadersSpec<?> headersSpec;
        if (method == HttpMethod.POST || method == HttpMethod.PUT || 
            method == HttpMethod.PATCH || method == HttpMethod.DELETE) {
            // Use cached body if available to avoid stream consumption issues
            Object cachedBody = exchange.getAttribute("cachedRequestBodyObject");
            if (cachedBody != null) {
                headersSpec = requestBodySpec.bodyValue(cachedBody);
            } else {
                headersSpec = requestBodySpec.body((outputMessage, context) -> 
                    outputMessage.writeWith(exchange.getRequest().getBody()));
            }
        } else {
            // For GET, HEAD, OPTIONS - no body
            headersSpec = requestBodySpec;
        }

        return headersSpec
                .exchangeToMono(clientResponse -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Request forwarded successfully - Method: {}, Path: {}, Status: {}, Duration: {}ms, RouteId: {}, ApiKeyId: {}", 
                            method, fullPath, clientResponse.statusCode(), duration, 
                            route != null ? route.getId() : null, apiKeyId);
                    
                    exchange.getResponse().setStatusCode(clientResponse.statusCode());
                    
                    // Copy headers from backend response (avoiding ReadOnlyHttpHeaders issue)
                    clientResponse.headers().asHttpHeaders().forEach((key, values) -> {
                        // Skip certain headers that should not be forwarded
                        if (!key.equalsIgnoreCase("Transfer-Encoding") && 
                            !key.equalsIgnoreCase("Connection") &&
                            !key.equalsIgnoreCase("Keep-Alive")) {
                            // Add each value individually to avoid ReadOnlyHttpHeaders issues
                            values.forEach(value -> exchange.getResponse().getHeaders().add(key, value));
                        }
                    });
                    
                    return exchange.getResponse()
                            .writeWith(clientResponse.bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class));
                })
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(e -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Error forwarding request - Method: {}, Path: {}, TargetUrl: {}, Duration: {}ms, RouteId: {}, ApiKeyId: {}, Error: {}", 
                            method, fullPath, completeUrl, duration, 
                            route != null ? route.getId() : null, apiKeyId, e.getMessage(), e);
                    
                    // Don't set response if already committed
                    if (exchange.getResponse().isCommitted()) {
                        log.warn("Response already committed, cannot set error status");
                        return exchange.getResponse().setComplete();
                    }
                    
                    exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() {
        return -70;
    }

    /**
     * Extract the remaining path after the matched publicPath pattern.
     * For example:
     *   fullPath: /api/users/123
     *   publicPath: /api/users/**
     *   result: /123
     * 
     * If fullPath matches basePath exactly, return empty string.
     * If publicPath ends with /**, remove it and extract the remaining segment.
     */
    private String extractRemainingPath(String fullPath, String publicPath) {
        if (publicPath == null || fullPath == null) {
            return fullPath;
        }

        // Remove /** wildcard suffix if present
        String basePath = publicPath.endsWith("/**") 
            ? publicPath.substring(0, publicPath.length() - 3)
            : publicPath;

        // If fullPath equals basePath exactly, no remaining path
        if (fullPath.equals(basePath)) {
            return "";
        }

        // If fullPath starts with basePath, extract the remaining part
        if (fullPath.startsWith(basePath)) {
            String remaining = fullPath.substring(basePath.length());
            // Ensure it starts with / if not empty
            return remaining.isEmpty() || remaining.startsWith("/") ? remaining : "/" + remaining;
        }

        // Fallback: return full path
        return fullPath;
    }
}