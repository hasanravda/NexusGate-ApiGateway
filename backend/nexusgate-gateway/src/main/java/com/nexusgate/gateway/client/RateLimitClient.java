package com.nexusgate.gateway.client;

import com.nexusgate.gateway.dto.EffectiveRateLimitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitClient {

    private final WebClient configServiceWebClient;

    public Mono<EffectiveRateLimitResponse> checkRateLimit(Long apiKeyId, Long serviceRouteId) {
        return configServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rate-limits/check")
                        .queryParam("apiKeyId", apiKeyId)
                        .queryParam("serviceRouteId", serviceRouteId)
                        .build())
                .retrieve()
                .bodyToMono(EffectiveRateLimitResponse.class)
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> log.error("Failed to check rate limit for apiKeyId: {}, serviceRouteId: {}",
                        apiKeyId, serviceRouteId, e))
                .onErrorResume(e -> Mono.empty());
    }
}