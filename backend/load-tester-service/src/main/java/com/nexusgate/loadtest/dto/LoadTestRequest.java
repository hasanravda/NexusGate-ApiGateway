package com.nexusgate.loadtest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for initiating a load test.
 * Contains all configuration parameters needed to simulate traffic.
 */
public class LoadTestRequest {

    @NotBlank(message = "Target API key is required")
    private String targetKey;

    @NotBlank(message = "Target endpoint URL is required")
    private String targetEndpoint;

    @NotNull(message = "Request rate is required")
    @Min(value = 1, message = "Request rate must be at least 1 req/sec")
    private Integer requestRate;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    private Integer durationSeconds;

    @NotNull(message = "Concurrency level is required")
    @Min(value = 1, message = "At least 1 concurrent client required")
    private Integer concurrencyLevel;

    @NotNull(message = "Request pattern is required")
    private RequestPattern requestPattern;

    @NotNull(message = "HTTP method is required")
    private HttpMethod httpMethod;

    // Constructors
    public LoadTestRequest() {
    }

    public LoadTestRequest(String targetKey, String targetEndpoint, Integer requestRate,
                           Integer durationSeconds, Integer concurrencyLevel,
                           RequestPattern requestPattern, HttpMethod httpMethod) {
        this.targetKey = targetKey;
        this.targetEndpoint = targetEndpoint;
        this.requestRate = requestRate;
        this.durationSeconds = durationSeconds;
        this.concurrencyLevel = concurrencyLevel;
        this.requestPattern = requestPattern;
        this.httpMethod = httpMethod;
    }

    // Getters and Setters
    public String getTargetKey() {
        return targetKey;
    }

    public void setTargetKey(String targetKey) {
        this.targetKey = targetKey;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    public Integer getRequestRate() {
        return requestRate;
    }

    public void setRequestRate(Integer requestRate) {
        this.requestRate = requestRate;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public void setConcurrencyLevel(Integer concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }

    public RequestPattern getRequestPattern() {
        return requestPattern;
    }

    public void setRequestPattern(RequestPattern requestPattern) {
        this.requestPattern = requestPattern;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * Defines the traffic pattern for load generation.
     */
    public enum RequestPattern {
        CONSTANT_RATE,  // Steady request rate throughout the test
        BURST,          // Send all requests as fast as possible
        RAMP_UP         // Gradually increase request rate
    }

    /**
     * Supported HTTP methods for load testing.
     */
    public enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }
}
