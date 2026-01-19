package com.nexusgate.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusgate.gateway.client.ApiKeyClient;
import com.nexusgate.gateway.dto.ApiKeyResponse;
import com.nexusgate.gateway.dto.ServiceRouteResponse;
import com.nexusgate.gateway.security.JwtValidator;
import com.nexusgate.gateway.util.HeaderUtil;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final ApiKeyClient apiKeyClient;
    private final JwtValidator jwtValidator;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServiceRouteResponse route = exchange.getAttribute("serviceRoute");

        // Skip authentication if route doesn't require it
        if (route == null || route.getAuthRequired() == null || !route.getAuthRequired()) {
            return chain.filter(exchange);
        }

        String authType = route.getAuthType();
        if (authType == null) {
            return chain.filter(exchange);
        }

        // Handle different authentication types
        switch (authType.toUpperCase()) {
            case "API_KEY":
                return authenticateWithApiKey(exchange, chain);
            case "JWT":
                return authenticateWithJwt(exchange, chain);
            case "BOTH":
                return authenticateWithApiKey(exchange, chain)
                        .flatMap(ignored -> authenticateWithJwt(exchange, chain));
            default:
                log.warn("Unknown auth type: {}", authType);
                return chain.filter(exchange);
        }
    }

    private Mono<Void> authenticateWithApiKey(ServerWebExchange exchange, GatewayFilterChain chain) {
        String apiKey = HeaderUtil.extractApiKey(exchange.getRequest());

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Missing API key for path: {}", exchange.getRequest().getPath());
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "API key is required");
        }

        return apiKeyClient.validateApiKey(apiKey)
                .flatMap(apiKeyResponse -> {
                    if (!isValidApiKey(apiKeyResponse)) {
                        log.warn("Invalid or expired API key");
                        return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired API key");
                    }

                    // Store API key ID for downstream filters
                    exchange.getAttributes().put("apiKeyId", apiKeyResponse.getId());
                    log.debug("API key validated successfully for keyId: {}", apiKeyResponse.getId());
                    return chain.filter(exchange);
                })
                .onErrorResume(throwable -> {
                    log.error("Error during API key validation", throwable);
                    return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "API key validation failed");
                })
                .switchIfEmpty(writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Invalid API key"));
    }

    private Mono<Void> authenticateWithJwt(ServerWebExchange exchange, GatewayFilterChain chain) {
        String jwt = HeaderUtil.extractJwt(exchange.getRequest());

        if (jwt == null || jwt.isEmpty()) {
            log.warn("Missing JWT token for path: {}", exchange.getRequest().getPath());
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "JWT token is required");
        }

        boolean isValid = jwtValidator.validateToken(jwt);
        if (!isValid) {
            log.warn("Invalid or expired JWT token");
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        }

        log.debug("JWT validated successfully");
        return chain.filter(exchange);
    }

    private boolean isValidApiKey(ApiKeyResponse apiKeyResponse) {
        if (apiKeyResponse == null) {
            return false;
        }

        if (apiKeyResponse.getIsActive() == null || !apiKeyResponse.getIsActive()) {
            return false;
        }

        if (apiKeyResponse.getExpiresAt() != null) {
            return apiKeyResponse.getExpiresAt().isAfter(LocalDateTime.now());
        }

        return true;
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
        return -90;
    }
}
