package com.nexusgate.gateway.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized utility for creating WebFlux-safe error responses across all filters.
 * Ensures consistent error response format and proper handling of committed responses.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorResponseUtil {

    private final ObjectMapper objectMapper;

    /**
     * Creates a structured JSON error response and writes it to the exchange.
     * This method is WebFlux-safe and handles already committed responses gracefully.
     *
     * @param exchange The ServerWebExchange
     * @param status The HTTP status code
     * @param message The error message
     * @return Mono<Void> representing the completion of writing the response
     */
    public Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        // Check if response is already committed
        if (exchange.getResponse().isCommitted()) {
            log.warn("Response already committed for path: {}, cannot write error response", 
                    exchange.getRequest().getPath().value());
            return exchange.getResponse().setComplete();
        }

        // Set status code and Content-Type header
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

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
            log.error("Failed to write error response for path: {}", 
                    exchange.getRequest().getPath().value(), e);
            return exchange.getResponse().setComplete();
        }
    }
}
