package com.nexusgate.gateway.client;

import com.nexusgate.gateway.dto.ServiceRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRouteClient {

    private final WebClient configServiceWebClient;

    public Mono<ServiceRouteResponse> getRouteByPath(String requestPath) {
        return configServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/service-routes/by-path")
                        .queryParam("path", requestPath)
                        .build())
                .retrieve()
                .bodyToMono(ServiceRouteResponse.class)
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> log.error("Failed to get route for path: {}", requestPath, e))
                .onErrorResume(e -> Mono.empty());
    }
}
