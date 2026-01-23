package com.nexusgate.loadtest.tester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Rate Limit Test Runner
 * 
 * Purpose: Validate API Gateway rate limiting behavior by sending concurrent requests
 * and analyzing HTTP 429 (Too Many Requests) responses.
 * 
 * Features:
 * - Concurrent request execution using ExecutorService
 * - Thread-safe metrics collection
 * - Detailed logging of rate limit violations
 * - Analytics verification support
 * 
 * Usage:
 *   RateLimitTestResult result = testRunner.runRateLimitTest(config);
 */
@Component
public class RateLimitTestRunner {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitTestRunner.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final RestTemplate restTemplate;

    public RateLimitTestRunner() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Configuration for rate limit testing
     */
    public static class RateLimitTestConfig {
        private String targetEndpoint;
        private String apiKey;
        private int totalRequests;
        private int concurrentThreads;
        private int expectedRateLimit;  // Expected number of requests allowed
        private long delayBetweenRequestsMs;
        
        public RateLimitTestConfig(String targetEndpoint, String apiKey, 
                                   int totalRequests, int concurrentThreads, 
                                   int expectedRateLimit) {
            this.targetEndpoint = targetEndpoint;
            this.apiKey = apiKey;
            this.totalRequests = totalRequests;
            this.concurrentThreads = concurrentThreads;
            this.expectedRateLimit = expectedRateLimit;
            this.delayBetweenRequestsMs = 0; // No delay for burst testing
        }

        // Getters and setters
        public String getTargetEndpoint() { return targetEndpoint; }
        public void setTargetEndpoint(String targetEndpoint) { this.targetEndpoint = targetEndpoint; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
        public int getConcurrentThreads() { return concurrentThreads; }
        public void setConcurrentThreads(int concurrentThreads) { this.concurrentThreads = concurrentThreads; }
        public int getExpectedRateLimit() { return expectedRateLimit; }
        public void setExpectedRateLimit(int expectedRateLimit) { this.expectedRateLimit = expectedRateLimit; }
        public long getDelayBetweenRequestsMs() { return delayBetweenRequestsMs; }
        public void setDelayBetweenRequestsMs(long delayBetweenRequestsMs) { 
            this.delayBetweenRequestsMs = delayBetweenRequestsMs; 
        }
    }

    /**
     * Result of rate limit test execution
     */
    public static class RateLimitTestResult {
        private int totalRequests;
        private int successfulRequests;      // HTTP 2xx
        private int rateLimitedRequests;     // HTTP 429
        private int errorRequests;           // HTTP 5xx or other errors
        private int unauthorizedRequests;    // HTTP 401/403
        
        private long totalDurationMs;
        private double requestsPerSecond;
        
        private List<String> rateLimitViolations;
        private List<RequestDetail> requestDetails;
        
        private boolean testPassed;
        private String failureReason;

        public RateLimitTestResult() {
            this.rateLimitViolations = new ArrayList<>();
            this.requestDetails = new ArrayList<>();
        }

        // Getters and setters
        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
        public int getSuccessfulRequests() { return successfulRequests; }
        public void setSuccessfulRequests(int successfulRequests) { this.successfulRequests = successfulRequests; }
        public int getRateLimitedRequests() { return rateLimitedRequests; }
        public void setRateLimitedRequests(int rateLimitedRequests) { this.rateLimitedRequests = rateLimitedRequests; }
        public int getErrorRequests() { return errorRequests; }
        public void setErrorRequests(int errorRequests) { this.errorRequests = errorRequests; }
        public int getUnauthorizedRequests() { return unauthorizedRequests; }
        public void setUnauthorizedRequests(int unauthorizedRequests) { this.unauthorizedRequests = unauthorizedRequests; }
        public long getTotalDurationMs() { return totalDurationMs; }
        public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
        public double getRequestsPerSecond() { return requestsPerSecond; }
        public void setRequestsPerSecond(double requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }
        public List<String> getRateLimitViolations() { return rateLimitViolations; }
        public void setRateLimitViolations(List<String> rateLimitViolations) { 
            this.rateLimitViolations = rateLimitViolations; 
        }
        public List<RequestDetail> getRequestDetails() { return requestDetails; }
        public void setRequestDetails(List<RequestDetail> requestDetails) { this.requestDetails = requestDetails; }
        public boolean isTestPassed() { return testPassed; }
        public void setTestPassed(boolean testPassed) { this.testPassed = testPassed; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    }

    /**
     * Detail of individual request
     */
    public static class RequestDetail {
        private int requestNumber;
        private int statusCode;
        private long latencyMs;
        private LocalDateTime timestamp;
        private String statusMessage;

        public RequestDetail(int requestNumber, int statusCode, long latencyMs, String statusMessage) {
            this.requestNumber = requestNumber;
            this.statusCode = statusCode;
            this.latencyMs = latencyMs;
            this.timestamp = LocalDateTime.now();
            this.statusMessage = statusMessage;
        }

        // Getters
        public int getRequestNumber() { return requestNumber; }
        public int getStatusCode() { return statusCode; }
        public long getLatencyMs() { return latencyMs; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getStatusMessage() { return statusMessage; }

        @Override
        public String toString() {
            return String.format("[Req #%d] %s | Status: %d (%s) | Latency: %dms",
                    requestNumber, timestamp.format(TIME_FORMATTER), statusCode, statusMessage, latencyMs);
        }
    }

    /**
     * Main method to execute rate limit test
     * 
     * @param config Test configuration
     * @return Test result with metrics and validation status
     */
    public RateLimitTestResult runRateLimitTest(RateLimitTestConfig config) {
        logger.info("=".repeat(80));
        logger.info("Starting Rate Limit Test");
        logger.info("=".repeat(80));
        logger.info("Target Endpoint: {}", config.getTargetEndpoint());
        logger.info("API Key: {}***", config.getApiKey().substring(0, Math.min(10, config.getApiKey().length())));
        logger.info("Total Requests: {}", config.getTotalRequests());
        logger.info("Concurrent Threads: {}", config.getConcurrentThreads());
        logger.info("Expected Rate Limit: {} requests", config.getExpectedRateLimit());
        logger.info("=".repeat(80));

        RateLimitTestResult result = new RateLimitTestResult();
        result.setTotalRequests(config.getTotalRequests());

        // Thread-safe counters
        LongAdder successCounter = new LongAdder();
        LongAdder rateLimitCounter = new LongAdder();
        LongAdder errorCounter = new LongAdder();
        LongAdder unauthorizedCounter = new LongAdder();
        
        AtomicInteger requestCounter = new AtomicInteger(0);
        List<RequestDetail> details = new CopyOnWriteArrayList<>();
        List<String> violations = new CopyOnWriteArrayList<>();

        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(config.getConcurrentThreads());
        List<Future<Void>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Submit all requests
        for (int i = 0; i < config.getTotalRequests(); i++) {
            final int requestNum = i + 1;
            
            Future<Void> future = executor.submit(() -> {
                executeRequest(config, requestNum, successCounter, rateLimitCounter, 
                             errorCounter, unauthorizedCounter, requestCounter, details, violations);
                return null;
            });
            
            futures.add(future);

            // Optional: Add delay between request submissions
            if (config.getDelayBetweenRequestsMs() > 0) {
                try {
                    Thread.sleep(config.getDelayBetweenRequestsMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Wait for all requests to complete
        logger.info("Waiting for all {} requests to complete...", config.getTotalRequests());
        for (Future<Void> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.error("Request timed out", e);
                errorCounter.increment();
            } catch (Exception e) {
                logger.error("Request failed", e);
                errorCounter.increment();
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Populate result
        result.setSuccessfulRequests((int) successCounter.sum());
        result.setRateLimitedRequests((int) rateLimitCounter.sum());
        result.setErrorRequests((int) errorCounter.sum());
        result.setUnauthorizedRequests((int) unauthorizedCounter.sum());
        result.setTotalDurationMs(duration);
        result.setRequestsPerSecond((config.getTotalRequests() * 1000.0) / duration);
        result.setRequestDetails(new ArrayList<>(details));
        result.setRateLimitViolations(new ArrayList<>(violations));

        // Validate test results
        validateResults(config, result);

        // Print summary
        printTestSummary(config, result);

        return result;
    }

    /**
     * Execute a single HTTP request and record metrics
     */
    private void executeRequest(RateLimitTestConfig config, int requestNum,
                               LongAdder successCounter, LongAdder rateLimitCounter,
                               LongAdder errorCounter, LongAdder unauthorizedCounter,
                               AtomicInteger requestCounter, List<RequestDetail> details,
                               List<String> violations) {
        
        long requestStart = System.currentTimeMillis();
        
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", config.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Execute request
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getTargetEndpoint(),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            long latency = System.currentTimeMillis() - requestStart;
            int statusCode = response.getStatusCode().value();
            
            // Categorize response
            if (statusCode >= 200 && statusCode < 300) {
                successCounter.increment();
                int currentCount = requestCounter.incrementAndGet();
                
                RequestDetail detail = new RequestDetail(requestNum, statusCode, latency, "SUCCESS");
                details.add(detail);
                
                logger.info("✓ Request #{} SUCCESS - Status: {} | Latency: {}ms | Successful so far: {}", 
                           requestNum, statusCode, latency, currentCount);
                
            } else if (statusCode == 429) {
                rateLimitCounter.increment();
                int currentCount = requestCounter.get();
                
                RequestDetail detail = new RequestDetail(requestNum, statusCode, latency, "RATE_LIMITED");
                details.add(detail);
                
                String violation = String.format("Rate limit exceeded at request #%d (after %d successful requests)", 
                                                requestNum, currentCount);
                violations.add(violation);
                
                logger.warn("⚠ Request #{} RATE LIMITED (429) - Latency: {}ms | Blocked after {} successful requests",
                           requestNum, latency, currentCount);
                
            } else if (statusCode == 401 || statusCode == 403) {
                unauthorizedCounter.increment();
                RequestDetail detail = new RequestDetail(requestNum, statusCode, latency, 
                                                        statusCode == 401 ? "UNAUTHORIZED" : "FORBIDDEN");
                details.add(detail);
                logger.error("✗ Request #{} AUTH FAILED - Status: {} | Latency: {}ms", 
                           requestNum, statusCode, latency);
            } else {
                errorCounter.increment();
                RequestDetail detail = new RequestDetail(requestNum, statusCode, latency, "ERROR");
                details.add(detail);
                logger.error("✗ Request #{} ERROR - Status: {} | Latency: {}ms", 
                           requestNum, statusCode, latency);
            }

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - requestStart;
            errorCounter.increment();
            
            // Check if it's a 429 response wrapped in exception
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                rateLimitCounter.increment();
                errorCounter.decrement();
                
                int currentCount = requestCounter.get();
                RequestDetail detail = new RequestDetail(requestNum, 429, latency, "RATE_LIMITED");
                details.add(detail);
                
                String violation = String.format("Rate limit exceeded at request #%d (after %d successful requests)", 
                                                requestNum, currentCount);
                violations.add(violation);
                
                logger.warn("⚠ Request #{} RATE LIMITED (429) - Latency: {}ms | Blocked after {} successful requests",
                           requestNum, latency, currentCount);
            } else {
                RequestDetail detail = new RequestDetail(requestNum, 0, latency, "EXCEPTION: " + e.getMessage());
                details.add(detail);
                logger.error("✗ Request #{} FAILED - Exception: {} | Latency: {}ms", 
                           requestNum, e.getMessage(), latency);
            }
        }
    }

    /**
     * Validate test results against expected behavior
     */
    private void validateResults(RateLimitTestConfig config, RateLimitTestResult result) {
        boolean passed = true;
        StringBuilder failures = new StringBuilder();

        // Validation 1: Should have some successful requests
        if (result.getSuccessfulRequests() == 0) {
            passed = false;
            failures.append("FAIL: No successful requests (expected at least some to succeed). ");
        }

        // Validation 2: Should have rate-limited requests when exceeding limit
        if (result.getTotalRequests() > config.getExpectedRateLimit() && result.getRateLimitedRequests() == 0) {
            passed = false;
            failures.append(String.format("FAIL: No rate-limited requests (sent %d requests, limit is %d). ",
                    result.getTotalRequests(), config.getExpectedRateLimit()));
        }

        // Validation 3: Successful requests should not exceed rate limit significantly
        int tolerance = (int) (config.getExpectedRateLimit() * 0.1); // 10% tolerance
        if (result.getSuccessfulRequests() > config.getExpectedRateLimit() + tolerance) {
            passed = false;
            failures.append(String.format("FAIL: Too many successful requests (%d) exceeded limit (%d + tolerance). ",
                    result.getSuccessfulRequests(), config.getExpectedRateLimit()));
        }

        // Validation 4: Should not have unauthorized errors with valid API key
        if (result.getUnauthorizedRequests() > 0) {
            passed = false;
            failures.append(String.format("FAIL: %d unauthorized requests (check API key validity). ",
                    result.getUnauthorizedRequests()));
        }

        result.setTestPassed(passed);
        result.setFailureReason(passed ? null : failures.toString());
    }

    /**
     * Print comprehensive test summary
     */
    private void printTestSummary(RateLimitTestConfig config, RateLimitTestResult result) {
        logger.info("");
        logger.info("=".repeat(80));
        logger.info("RATE LIMIT TEST SUMMARY");
        logger.info("=".repeat(80));
        logger.info("Test Configuration:");
        logger.info("  Target Endpoint     : {}", config.getTargetEndpoint());
        logger.info("  Expected Rate Limit : {} requests", config.getExpectedRateLimit());
        logger.info("  Total Requests Sent : {}", config.getTotalRequests());
        logger.info("  Concurrent Threads  : {}", config.getConcurrentThreads());
        logger.info("");
        logger.info("Test Results:");
        logger.info("  ✓ Successful (2xx)  : {} requests", result.getSuccessfulRequests());
        logger.info("  ⚠ Rate Limited (429): {} requests", result.getRateLimitedRequests());
        logger.info("  ✗ Errors (5xx/other): {} requests", result.getErrorRequests());
        logger.info("  ✗ Unauthorized      : {} requests", result.getUnauthorizedRequests());
        logger.info("");
        logger.info("Performance Metrics:");
        logger.info("  Total Duration      : {}ms ({} seconds)", 
                   result.getTotalDurationMs(), result.getTotalDurationMs() / 1000.0);
        logger.info("  Throughput          : {:.2f} requests/second", result.getRequestsPerSecond());
        logger.info("");
        
        if (!result.getRateLimitViolations().isEmpty()) {
            logger.info("Rate Limit Violations:");
            result.getRateLimitViolations().forEach(v -> logger.info("  - {}", v));
            logger.info("");
        }

        logger.info("Test Validation: {}", result.isTestPassed() ? "✓ PASSED" : "✗ FAILED");
        if (!result.isTestPassed()) {
            logger.error("Failure Reason: {}", result.getFailureReason());
        }
        logger.info("=".repeat(80));
    }
}
