package com.nexusgate.loadtest.service;

import com.nexusgate.loadtest.dto.LoadTestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service for executing HTTP requests using WebClient (non-blocking).
 * Measures request latency and handles various HTTP methods.
 */
@Service
public class HttpClientService {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientService.class);
    
    private final WebClient webClient;
    
    // Timeout configuration
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    public HttpClientService() {
        this.webClient = WebClient.builder()
                .build();
    }

    /**
     * Executes a single HTTP request and measures its latency.
     * Returns a RequestResult containing status code and latency.
     *
     * @param endpoint Target URL
     * @param apiKey API key for authentication
     * @param method HTTP method to use
     * @return RequestResult with status code and latency
     */
    public Mono<RequestResult> executeRequest(String endpoint, String apiKey, LoadTestRequest.HttpMethod method) {
        long startTime = System.currentTimeMillis();

        return buildRequest(endpoint, apiKey, method)
                .retrieve()
                .toBodilessEntity()
                .map(response -> {
                    long latency = System.currentTimeMillis() - startTime;
                    int statusCode = response.getStatusCode().value();
                    return new RequestResult(statusCode, latency, true);
                })
                .onErrorResume(error -> {
                    // Handle errors (timeout, connection errors, etc.)
                    long latency = System.currentTimeMillis() - startTime;
                    logger.debug("Request failed: {}", error.getMessage());
                    
                    // Extract status code from WebClient exceptions if available
                    int statusCode = extractStatusCode(error);
                    return Mono.just(new RequestResult(statusCode, latency, false));
                })
                .timeout(REQUEST_TIMEOUT)
                .onErrorResume(java.util.concurrent.TimeoutException.class, e -> {
                    long latency = System.currentTimeMillis() - startTime;
                    logger.debug("Request timed out after {}ms", latency);
                    return Mono.just(new RequestResult(0, latency, false));
                });
    }

    /**
     * Builds a WebClient request based on HTTP method.
     */
    private WebClient.RequestHeadersSpec<?> buildRequest(String endpoint, String apiKey, 
                                                          LoadTestRequest.HttpMethod method) {
        WebClient.RequestHeadersSpec<?> request;

        switch (method) {
            case GET:
                request = webClient.get().uri(endpoint);
                break;
            case POST:
                request = webClient.post().uri(endpoint);
                break;
            case PUT:
                request = webClient.put().uri(endpoint);
                break;
            case DELETE:
                request = webClient.delete().uri(endpoint);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        // Add API key header
        return request.header("X-API-Key", apiKey);
    }

    /**
     * Extracts HTTP status code from WebClient exceptions.
     */
    private int extractStatusCode(Throwable error) {
        if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
            org.springframework.web.reactive.function.client.WebClientResponseException webClientError = 
                    (org.springframework.web.reactive.function.client.WebClientResponseException) error;
            return webClientError.getStatusCode().value();
        }
        return 0; // Unknown error (connection refused, DNS failure, etc.)
    }

    /**
     * Data class to hold request execution results.
     */
    public static class RequestResult {
        private final int statusCode;
        private final long latencyMs;
        private final boolean success;

        public RequestResult(int statusCode, long latencyMs, boolean success) {
            this.statusCode = statusCode;
            this.latencyMs = latencyMs;
            this.success = success;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public long getLatencyMs() {
            return latencyMs;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
