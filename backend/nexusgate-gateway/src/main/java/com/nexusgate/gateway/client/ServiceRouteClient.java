package com.nexusgate.gateway.client;

import com.nexusgate.gateway.dto.ServiceRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRouteClient {

    private final WebClient configServiceWebClient;

    /**
     * Fetch all active service routes from the configuration service.
     * Implements retry logic, timeout, and comprehensive error handling.
     */
    public Flux<ServiceRouteResponse> getAllActiveRoutes() {
        return configServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/service-routes")
                        .queryParam("activeOnly", true)
                        .build())
                .retrieve()
                .bodyToFlux(ServiceRouteResponse.class)
                .timeout(Duration.ofSeconds(10)) // Increased timeout to 10 seconds
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                        .maxBackoff(Duration.ofSeconds(2))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.NotFound))
                        .doBeforeRetry(retrySignal -> 
                            log.warn("Retrying config service call, attempt: {}", retrySignal.totalRetries() + 1)))
                .doOnError(WebClientResponseException.class, e -> {
                    log.error("Config service returned error status: {} - {}", 
                            e.getStatusCode(), e.getResponseBodyAsString());
                })
                .doOnError(java.util.concurrent.TimeoutException.class, e -> {
                    log.error("Timeout calling config service after 10 seconds. Config service may be slow or down.");
                })
                .doOnError(java.net.ConnectException.class, e -> {
                    log.error("Cannot connect to config service. Service may be down or unreachable.");
                })
                .doOnError(e -> {
                    if (!(e instanceof WebClientResponseException) && 
                        !(e instanceof java.util.concurrent.TimeoutException) &&
                        !(e instanceof java.net.ConnectException)) {
                        log.error("Unexpected error fetching routes from config service: {}", e.getMessage(), e);
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Falling back to empty route list due to error. Gateway will return 404 for all requests.");
                    return Flux.empty(); // Return empty list on any error
                });
    }
}

