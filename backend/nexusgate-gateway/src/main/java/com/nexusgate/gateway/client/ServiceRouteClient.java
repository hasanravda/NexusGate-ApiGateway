package com.nexusgate.gateway.client;

import com.nexusgate.gateway.dto.ServiceRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRouteClient {

    private final WebClient configServiceWebClient;

    /**
     * Fetch all active service routes from the configuration service.
     * Implements retry logic, timeout, and comprehensive error handling.
     * Returns empty list if config service is unavailable (graceful degradation).
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
                .timeout(Duration.ofSeconds(5))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                        .maxBackoff(Duration.ofSeconds(2))
                        // Only retry on server errors (5xx), not on connection errors
                        .filter(throwable -> {
                            if (throwable instanceof WebClientResponseException) {
                                WebClientResponseException ex = (WebClientResponseException) throwable;
                                return ex.getStatusCode().is5xxServerError();
                            }
                            // Don't retry on connection refused or timeout - service is down
                            return false;
                        })
                        .doBeforeRetry(retrySignal -> 
                            log.debug("Retrying config service call, attempt: {}", retrySignal.totalRetries() + 1)))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.debug("Config service error: {} - Returning empty routes", e.getStatusCode());
                    return Flux.empty();
                })
                .onErrorResume(TimeoutException.class, e -> {
                    log.debug("Config service timeout - Returning empty routes");
                    return Flux.empty();
                })
                .onErrorResume(WebClientRequestException.class, e -> {
                    if (e.getCause() instanceof ConnectException) {
                        log.debug("Config service unavailable (connection refused) - Returning empty routes");
                    } else {
                        log.debug("Config service error: {} - Returning empty routes", e.getMessage());
                    }
                    return Flux.empty();
                })
                .onErrorResume(e -> {
                    // Catch all other errors and return empty (graceful degradation)
                    log.debug("Config service call failed - Returning empty routes");
                    return Flux.empty();
                });
    }
}
