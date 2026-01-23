package com.nexusgate.loadtest.controller;

import com.nexusgate.loadtest.tester.AnalyticsVerifier;
import com.nexusgate.loadtest.tester.RateLimitTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limit Testing Controller
 * 
 * Provides REST endpoints to execute rate limit validation tests
 * and verify analytics data.
 * 
 * Endpoints:
 *   POST /rate-limit-test/execute - Execute a rate limit test
 *   POST /rate-limit-test/verify-analytics - Verify analytics after test
 *   POST /rate-limit-test/full-test - Execute test + verify analytics
 */
@RestController
@RequestMapping("/rate-limit-test")
public class RateLimitTestController {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitTestController.class);

    private final RateLimitTestRunner testRunner;
    private final AnalyticsVerifier analyticsVerifier;

    // Store last test result for analytics verification
    private RateLimitTestRunner.RateLimitTestResult lastTestResult;

    public RateLimitTestController(RateLimitTestRunner testRunner, AnalyticsVerifier analyticsVerifier) {
        this.testRunner = testRunner;
        this.analyticsVerifier = analyticsVerifier;
    }

    /**
     * Execute rate limit test
     * 
     * POST /rate-limit-test/execute
     * {
     *   "targetEndpoint": "http://localhost:8081/api/users",
     *   "apiKey": "nx_test_key_12345",
     *   "totalRequests": 50,
     *   "concurrentThreads": 10,
     *   "expectedRateLimit": 20,
     *   "delayBetweenRequestsMs": 0
     * }
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeRateLimitTest(@RequestBody Map<String, Object> request) {
        logger.info("Received rate limit test request");

        try {
            // Parse configuration
            RateLimitTestRunner.RateLimitTestConfig config = parseConfig(request);

            // Execute test
            RateLimitTestRunner.RateLimitTestResult result = testRunner.runRateLimitTest(config);
            
            // Store for potential analytics verification
            this.lastTestResult = result;

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("testPassed", result.isTestPassed());
            response.put("totalRequests", result.getTotalRequests());
            response.put("successfulRequests", result.getSuccessfulRequests());
            response.put("rateLimitedRequests", result.getRateLimitedRequests());
            response.put("errorRequests", result.getErrorRequests());
            response.put("unauthorizedRequests", result.getUnauthorizedRequests());
            response.put("durationMs", result.getTotalDurationMs());
            response.put("requestsPerSecond", result.getRequestsPerSecond());
            response.put("rateLimitViolations", result.getRateLimitViolations());
            response.put("failureReason", result.getFailureReason());
            
            response.put("message", result.isTestPassed() 
                    ? "Rate limit test completed successfully" 
                    : "Rate limit test failed validation");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to execute rate limit test", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Test execution failed");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify analytics data against last test
     * 
     * POST /rate-limit-test/verify-analytics
     * {
     *   "analyticsBaseUrl": "http://localhost:8085"
     * }
     */
    @PostMapping("/verify-analytics")
    public ResponseEntity<Map<String, Object>> verifyAnalytics(@RequestBody Map<String, Object> request) {
        logger.info("Received analytics verification request");

        if (lastTestResult == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "No test result available");
            errorResponse.put("message", "Execute a test first using /rate-limit-test/execute");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            String analyticsBaseUrl = (String) request.get("analyticsBaseUrl");
            if (analyticsBaseUrl == null || analyticsBaseUrl.isEmpty()) {
                analyticsBaseUrl = "http://localhost:8085";
            }

            // Verify analytics
            AnalyticsVerifier.AnalyticsVerificationResult result = 
                    analyticsVerifier.verifyAnalytics(analyticsBaseUrl, lastTestResult);
            
            analyticsVerifier.printVerificationSummary(result);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("verified", result.isVerified());
            response.put("message", result.getMessage());
            response.put("analyticsTotal", result.getAnalyticsTotal());
            response.put("analyticsBlocked", result.getAnalyticsBlocked());
            response.put("expectedTotal", result.getExpectedTotal());
            response.put("expectedBlocked", result.getExpectedBlocked());
            response.put("totalMatches", result.isTotalMatches());
            response.put("blockedMatches", result.isBlockedMatches());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to verify analytics", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Analytics verification failed");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Execute full test: run rate limit test + verify analytics
     * 
     * POST /rate-limit-test/full-test
     * {
     *   "targetEndpoint": "http://localhost:8081/api/users",
     *   "apiKey": "nx_test_key_12345",
     *   "totalRequests": 50,
     *   "concurrentThreads": 10,
     *   "expectedRateLimit": 20,
     *   "delayBetweenRequestsMs": 0,
     *   "analyticsBaseUrl": "http://localhost:8085",
     *   "waitForAnalyticsMs": 2000
     * }
     */
    @PostMapping("/full-test")
    public ResponseEntity<Map<String, Object>> executeFullTest(@RequestBody Map<String, Object> request) {
        logger.info("Received full rate limit test request (test + analytics verification)");

        try {
            // Parse configuration
            RateLimitTestRunner.RateLimitTestConfig config = parseConfig(request);

            // Execute test
            logger.info("Step 1: Executing rate limit test...");
            RateLimitTestRunner.RateLimitTestResult testResult = testRunner.runRateLimitTest(config);
            this.lastTestResult = testResult;

            // Wait for analytics to process (optional)
            int waitMs = getIntValue(request, "waitForAnalyticsMs", 2000);
            if (waitMs > 0) {
                logger.info("Step 2: Waiting {}ms for analytics to process...", waitMs);
                Thread.sleep(waitMs);
            }

            // Verify analytics
            logger.info("Step 3: Verifying analytics...");
            String analyticsBaseUrl = (String) request.getOrDefault("analyticsBaseUrl", "http://localhost:8085");
            AnalyticsVerifier.AnalyticsVerificationResult analyticsResult = 
                    analyticsVerifier.verifyAnalytics(analyticsBaseUrl, testResult);
            
            analyticsVerifier.printVerificationSummary(analyticsResult);

            // Build comprehensive response
            Map<String, Object> response = new HashMap<>();
            
            // Test results
            Map<String, Object> testData = new HashMap<>();
            testData.put("testPassed", testResult.isTestPassed());
            testData.put("totalRequests", testResult.getTotalRequests());
            testData.put("successfulRequests", testResult.getSuccessfulRequests());
            testData.put("rateLimitedRequests", testResult.getRateLimitedRequests());
            testData.put("errorRequests", testResult.getErrorRequests());
            testData.put("durationMs", testResult.getTotalDurationMs());
            testData.put("requestsPerSecond", testResult.getRequestsPerSecond());
            testData.put("failureReason", testResult.getFailureReason());
            response.put("rateLimitTest", testData);
            
            // Analytics verification results
            Map<String, Object> analyticsData = new HashMap<>();
            analyticsData.put("verified", analyticsResult.isVerified());
            analyticsData.put("message", analyticsResult.getMessage());
            analyticsData.put("analyticsTotal", analyticsResult.getAnalyticsTotal());
            analyticsData.put("analyticsBlocked", analyticsResult.getAnalyticsBlocked());
            analyticsData.put("totalMatches", analyticsResult.isTotalMatches());
            analyticsData.put("blockedMatches", analyticsResult.isBlockedMatches());
            response.put("analyticsVerification", analyticsData);
            
            // Overall status
            boolean overallPassed = testResult.isTestPassed() && analyticsResult.isVerified();
            response.put("overallStatus", overallPassed ? "PASSED" : "FAILED");
            response.put("message", overallPassed 
                    ? "✓ Full rate limit test PASSED - Test and analytics verification successful"
                    : "✗ Full rate limit test FAILED - Check test results and analytics verification");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to execute full rate limit test", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Full test execution failed");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get test status/info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Rate Limit Testing Service");
        info.put("version", "1.0.0");
        info.put("description", "Validates API Gateway rate limiting and analytics tracking");
        info.put("lastTestAvailable", lastTestResult != null);
        
        if (lastTestResult != null) {
            info.put("lastTestSummary", Map.of(
                    "totalRequests", lastTestResult.getTotalRequests(),
                    "successfulRequests", lastTestResult.getSuccessfulRequests(),
                    "rateLimitedRequests", lastTestResult.getRateLimitedRequests(),
                    "testPassed", lastTestResult.isTestPassed()
            ));
        }
        
        info.put("endpoints", Map.of(
                "executeTest", "POST /rate-limit-test/execute",
                "verifyAnalytics", "POST /rate-limit-test/verify-analytics",
                "fullTest", "POST /rate-limit-test/full-test",
                "info", "GET /rate-limit-test/info"
        ));
        
        return ResponseEntity.ok(info);
    }

    /**
     * Parse configuration from request body
     */
    private RateLimitTestRunner.RateLimitTestConfig parseConfig(Map<String, Object> request) {
        String targetEndpoint = (String) request.get("targetEndpoint");
        String apiKey = (String) request.get("apiKey");
        int totalRequests = getIntValue(request, "totalRequests", 50);
        int concurrentThreads = getIntValue(request, "concurrentThreads", 10);
        int expectedRateLimit = getIntValue(request, "expectedRateLimit", 20);
        long delayBetweenRequestsMs = getLongValue(request, "delayBetweenRequestsMs", 0);

        RateLimitTestRunner.RateLimitTestConfig config = new RateLimitTestRunner.RateLimitTestConfig(
                targetEndpoint, apiKey, totalRequests, concurrentThreads, expectedRateLimit
        );
        config.setDelayBetweenRequestsMs(delayBetweenRequestsMs);

        return config;
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}
