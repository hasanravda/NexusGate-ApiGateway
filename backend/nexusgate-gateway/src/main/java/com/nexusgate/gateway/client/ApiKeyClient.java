package com.nexusgate.gateway.client;

import com.nexusgate.gateway.dto.ApiKeyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

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
                .bodyToMono(ApiKeyResponse.class)
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> log.error("Failed to validate API key", e))
                .onErrorResume(e -> Mono.empty());
    }
}