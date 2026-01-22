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
                    return new RequestResult(statusCode, latency, true, null);
                })
                .onErrorResume(error -> {
                    // Handle errors (timeout, connection errors, etc.)
                    long latency = System.currentTimeMillis() - startTime;
                    
                    // Extract status code and error message from WebClient exceptions
                    int statusCode = extractStatusCode(error);
                    String errorMessage = extractErrorMessage(error, statusCode);
                    
                    // Log detailed error information
                    if (statusCode == 401) {
                        logger.error("Authentication failed (401 Unauthorized) - API Key may be invalid or inactive. " +
                                    "Endpoint: {}, Method: {}, API Key: {}***", 
                                    endpoint, method, apiKey != null && apiKey.length() > 6 ? apiKey.substring(0, 6) : "null");
                    } else if (statusCode == 403) {
                        logger.error("Authorization failed (403 Forbidden) - API Key lacks permission for this endpoint. " +
                                    "Endpoint: {}, Method: {}", endpoint, method);
                    } else if (statusCode == 429) {
                        logger.debug("Rate limit exceeded (429 Too Many Requests) - Endpoint: {}", endpoint);
                    } else if (statusCode >= 500) {
                        logger.error("Server error ({}) - Endpoint: {}, Error: {}", statusCode, endpoint, errorMessage);
                    } else if (statusCode > 0) {
                        logger.warn("Request failed with status {} - Endpoint: {}, Error: {}", statusCode, endpoint, errorMessage);
                    } else {
                        logger.error("Connection error - Endpoint: {}, Error: {}", endpoint, errorMessage);
                    }
                    
                    return Mono.just(new RequestResult(statusCode, latency, false, errorMessage));
                })
                .timeout(REQUEST_TIMEOUT)
                .onErrorResume(java.util.concurrent.TimeoutException.class, e -> {
                    long latency = System.currentTimeMillis() - startTime;
                    logger.error("Request timed out after {}ms - Endpoint: {}", latency, endpoint);
                    return Mono.just(new RequestResult(0, latency, false, "Request timeout"));
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
     * Extracts error message from exceptions.
     */
    private String extractErrorMessage(Throwable error, int statusCode) {
        if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
            org.springframework.web.reactive.function.client.WebClientResponseException webClientError = 
                    (org.springframework.web.reactive.function.client.WebClientResponseException) error;
            
            String responseBody = webClientError.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isEmpty()) {
                return String.format("HTTP %d: %s", statusCode, responseBody);
            }
            return String.format("HTTP %d: %s", statusCode, webClientError.getStatusText());
        }
        
        // Handle other exceptions
        if (error instanceof java.net.ConnectException) {
            return "Connection refused - Target service may be down";
        } else if (error instanceof java.net.UnknownHostException) {
            return "Unknown host - Check endpoint URL";
        } else if (error instanceof java.util.concurrent.TimeoutException) {
            return "Request timeout";
        }
        
        return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
    }

    /**
     * Data class to hold request execution results.
     */
    public static class RequestResult {
        private final int statusCode;
        private final long latencyMs;
        private final boolean success;
        private final String errorMessage;

        public RequestResult(int statusCode, long latencyMs, boolean success, String errorMessage) {
            this.statusCode = statusCode;
            this.latencyMs = latencyMs;
            this.success = success;
            this.errorMessage = errorMessage;
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

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
