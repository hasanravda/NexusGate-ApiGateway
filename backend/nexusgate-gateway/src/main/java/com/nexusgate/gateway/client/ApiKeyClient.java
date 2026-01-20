package com.nexusgate.gateway.client;

import com.nexusgate.gateway.dto.ApiKeyResponse;
import com.nexusgate.gateway.exception.ApiKeyInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyClient {

    private final WebClient configServiceWebClient;

    public Mono<ApiKeyResponse> validateApiKey(String keyValue) {
        return configServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/keys/validate")
                        .queryParam("keyValue", keyValue)
                        .build())
                .retrieve()
                .onStatus(status -> status.value() == 404, 
                    response -> {
                        log.warn("API key not found: {}...", 
                                keyValue != null && keyValue.length() > 4 ? keyValue.substring(0, 4) : "null");
                        return Mono.error(new ApiKeyInvalidException("API key not found"));
                    })
                .bodyToMono(ApiKeyResponse.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(response -> log.debug("API key validation successful for key: {}...", 
                        keyValue != null && keyValue.length() > 4 ? keyValue.substring(0, 4) : "null"))
                .onErrorResume(TimeoutException.class, e -> {
                    log.error("Timeout validating API key with config service");
                    return Mono.error(new RuntimeException("Config service timeout"));
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Config service returned error status: {} for API key validation", e.getStatusCode());
                    return Mono.error(new RuntimeException("Config service error: " + e.getStatusCode()));
                })
                .onErrorResume(e -> {
                    if (e instanceof ApiKeyInvalidException) {
                        return Mono.error(e);
                    }
                    log.error("Failed to validate API key with config service: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Config service unavailable: " + e.getMessage()));
                });
    }
}