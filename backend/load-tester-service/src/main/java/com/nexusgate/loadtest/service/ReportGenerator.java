package com.nexusgate.loadtest.service;

import com.nexusgate.loadtest.dto.LoadTestResult;
import com.nexusgate.loadtest.model.TestExecution;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Generates comprehensive load test reports from collected metrics.
 * Converts raw metrics into a structured LoadTestResult DTO.
 */
@Service
public class ReportGenerator {

    /**
     * Generates a complete test result report from a finished test execution.
     *
     * @param execution Completed test execution
     * @return Aggregated test results
     */
    public LoadTestResult generateReport(TestExecution execution) {
        LoadTestResult result = new LoadTestResult();
        MetricsCollector metrics = execution.getMetricsCollector();

        // Basic test info
        result.setTestId(execution.getTestId());
        result.setStatus(execution.getStatus());
        result.setStartTime(execution.getStartTime());
        result.setEndTime(execution.getEndTime());

        // Calculate duration
        if (execution.getEndTime() != null) {
            Duration duration = Duration.between(execution.getStartTime(), execution.getEndTime());
            result.setDurationMs(duration.toMillis());
        }

        // Request metrics
        result.setTotalRequests(metrics.getTotalRequests());
        result.setSuccessfulRequests(metrics.getSuccessfulRequests());
        result.setRateLimitedRequests(metrics.getRateLimitedRequests());
        result.setErrorRequests(metrics.getErrorRequests());

        // Latency metrics
        result.setAverageLatencyMs(metrics.getAverageLatencyMs());
        result.setP95LatencyMs(metrics.getP95LatencyMs());
        result.setMinLatencyMs(metrics.getMinLatencyMs());
        result.setMaxLatencyMs(metrics.getMaxLatencyMs());

        // Calculate rates
        long totalRequests = metrics.getTotalRequests();
        if (totalRequests > 0) {
            // Throughput (requests per second)
            double durationSeconds = result.getDurationMs() / 1000.0;
            if (durationSeconds > 0) {
                result.setRequestsPerSecond(totalRequests / durationSeconds);
            }

            // Success rate (percentage of 2xx responses)
            result.setSuccessRate((metrics.getSuccessfulRequests() * 100.0) / totalRequests);

            // Rate limit rate (percentage of 429 responses)
            result.setRateLimitRate((metrics.getRateLimitedRequests() * 100.0) / totalRequests);
        } else {
            result.setRequestsPerSecond(0.0);
            result.setSuccessRate(0.0);
            result.setRateLimitRate(0.0);
        }

        // Test configuration summary
        result.setTargetEndpoint(execution.getRequest().getTargetEndpoint());
        result.setConfiguredRequestRate(execution.getRequest().getRequestRate());
        result.setConcurrencyLevel(execution.getRequest().getConcurrencyLevel());

        return result;
    }

    /**
     * Generates a partial report for a running test (current status).
     * Useful for real-time monitoring.
     */
    public LoadTestResult generatePartialReport(TestExecution execution) {
        LoadTestResult result = new LoadTestResult();
        MetricsCollector metrics = execution.getMetricsCollector();

        result.setTestId(execution.getTestId());
        result.setStatus(execution.getStatus());
        result.setStartTime(execution.getStartTime());

        // Current metrics snapshot
        result.setTotalRequests(metrics.getTotalRequests());
        result.setSuccessfulRequests(metrics.getSuccessfulRequests());
        result.setRateLimitedRequests(metrics.getRateLimitedRequests());
        result.setErrorRequests(metrics.getErrorRequests());

        // Current latency metrics
        result.setAverageLatencyMs(metrics.getAverageLatencyMs());
        result.setP95LatencyMs(metrics.getP95LatencyMs());

        // Calculate current throughput
        Duration elapsed = Duration.between(execution.getStartTime(), LocalDateTime.now());
        double elapsedSeconds = elapsed.toMillis() / 1000.0;
        if (elapsedSeconds > 0) {
            result.setRequestsPerSecond(metrics.getTotalRequests() / elapsedSeconds);
        }

        // Test config
        result.setTargetEndpoint(execution.getRequest().getTargetEndpoint());
        result.setConfiguredRequestRate(execution.getRequest().getRequestRate());
        result.setConcurrencyLevel(execution.getRequest().getConcurrencyLevel());

        return result;
    }
}
