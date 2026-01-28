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
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.NotFound))
                        .doBeforeRetry(retrySignal -> 
                            log.debug("Retrying config service call, attempt: {}", retrySignal.totalRetries() + 1)))
                .doOnError(WebClientResponseException.class, e -> {
                    log.debug("Config service returned error status: {} - {}", 
                            e.getStatusCode(), e.getResponseBodyAsString());
                })
                .doOnError(TimeoutException.class, e -> {
                    log.debug("Config service request timeout (may be starting up or slow)");
                })
                .doOnError(WebClientRequestException.class, e -> {
                    if (e.getCause() instanceof ConnectException) {
                        log.debug("Config service not reachable (connection refused)");
                    } else {
                        log.debug("Config service request error: {}", e.getMessage());
                    }
                })
                .doOnError(e -> {
                    // Log unexpected errors at debug level to avoid noise
                    if (!(e instanceof WebClientResponseException) && 
                        !(e instanceof TimeoutException) &&
                        !(e instanceof WebClientRequestException)) {
                        log.debug("Error fetching routes: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }
                })
                .onErrorResume(e -> {
                    // Graceful degradation - return empty route list
                    // This allows gateway to start without config service
                    return Flux.empty();
                });
    }
}
