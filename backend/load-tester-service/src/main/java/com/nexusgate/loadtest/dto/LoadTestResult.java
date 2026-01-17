package com.nexusgate.loadtest.dto;

import java.time.LocalDateTime;

/**
 * Aggregated results of a completed load test.
 * Contains all metrics collected during test execution.
 */
public class LoadTestResult {

    private String testId;
    private LoadTestStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    // Request metrics
    private Long totalRequests;
    private Long successfulRequests;    // HTTP 2xx
    private Long rateLimitedRequests;   // HTTP 429
    private Long errorRequests;         // HTTP 5xx, timeouts, etc.

    // Latency metrics (in milliseconds)
    private Double averageLatencyMs;
    private Long p95LatencyMs;
    private Long minLatencyMs;
    private Long maxLatencyMs;

    // Throughput metrics
    private Double requestsPerSecond;
    private Double successRate;
    private Double rateLimitRate;

    // Test configuration summary
    private String targetEndpoint;
    private Integer configuredRequestRate;
    private Integer concurrencyLevel;

    // Constructors
    public LoadTestResult() {
    }

    // Getters and Setters
    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public LoadTestStatus getStatus() {
        return status;
    }

    public void setStatus(LoadTestStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(Long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public Long getSuccessfulRequests() {
        return successfulRequests;
    }

    public void setSuccessfulRequests(Long successfulRequests) {
        this.successfulRequests = successfulRequests;
    }

    public Long getRateLimitedRequests() {
        return rateLimitedRequests;
    }

    public void setRateLimitedRequests(Long rateLimitedRequests) {
        this.rateLimitedRequests = rateLimitedRequests;
    }

    public Long getErrorRequests() {
        return errorRequests;
    }

    public void setErrorRequests(Long errorRequests) {
        this.errorRequests = errorRequests;
    }

    public Double getAverageLatencyMs() {
        return averageLatencyMs;
    }

    public void setAverageLatencyMs(Double averageLatencyMs) {
        this.averageLatencyMs = averageLatencyMs;
    }

    public Long getP95LatencyMs() {
        return p95LatencyMs;
    }

    public void setP95LatencyMs(Long p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
    }

    public Long getMinLatencyMs() {
        return minLatencyMs;
    }

    public void setMinLatencyMs(Long minLatencyMs) {
        this.minLatencyMs = minLatencyMs;
    }

    public Long getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public void setMaxLatencyMs(Long maxLatencyMs) {
        this.maxLatencyMs = maxLatencyMs;
    }

    public Double getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(Double requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public Double getRateLimitRate() {
        return rateLimitRate;
    }

    public void setRateLimitRate(Double rateLimitRate) {
        this.rateLimitRate = rateLimitRate;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    public Integer getConfiguredRequestRate() {
        return configuredRequestRate;
    }

    public void setConfiguredRequestRate(Integer configuredRequestRate) {
        this.configuredRequestRate = configuredRequestRate;
    }

    public Integer getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public void setConcurrencyLevel(Integer concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }
}
