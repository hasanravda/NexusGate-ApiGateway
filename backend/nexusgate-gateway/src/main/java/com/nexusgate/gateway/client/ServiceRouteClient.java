package com.nexusgate.gateway.client;

import com.nexusgate.gateway.dto.ServiceRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRouteClient {

    private final WebClient configServiceWebClient;

    public Flux<ServiceRouteResponse> getAllActiveRoutes() {
    // Fetch all active service routes from the configuration service
        return configServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/service-routes")
                        .queryParam("activeOnly", true)
                        .build())
                .retrieve()
                .bodyToFlux(ServiceRouteResponse.class)
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> log.error("Failed to fetch active routes from config service", e))
                .onErrorResume(e -> Flux.empty());
    }
}
