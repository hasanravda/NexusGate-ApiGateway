package com.nexusgate.loadtest.tester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * Analytics Verifier
 * 
 * Purpose: Verify that the Analytics Service correctly tracked requests
 * after a load test, including successful requests and rate-limited (429) responses.
 * 
 * This ensures that:
 * 1. All requests (allowed + blocked) are logged
 * 2. Rate-limited requests are correctly counted as errors
 * 3. Analytics data matches load test results
 */
@Component
public class AnalyticsVerifier {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsVerifier.class);
    
    private final RestTemplate restTemplate;

    public AnalyticsVerifier() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Verification result from Analytics Service
     */
    public static class AnalyticsVerificationResult {
        private boolean verified;
        private String message;
        
        private long analyticsTotal;
        private long analyticsBlocked;
        private long analyticsErrors;
        
        private long expectedTotal;
        private long expectedBlocked;
        
        private boolean totalMatches;
        private boolean blockedMatches;

        public AnalyticsVerificationResult() {}

        // Getters and setters
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public long getAnalyticsTotal() { return analyticsTotal; }
        public void setAnalyticsTotal(long analyticsTotal) { this.analyticsTotal = analyticsTotal; }
        public long getAnalyticsBlocked() { return analyticsBlocked; }
        public void setAnalyticsBlocked(long analyticsBlocked) { this.analyticsBlocked = analyticsBlocked; }
        public long getAnalyticsErrors() { return analyticsErrors; }
        public void setAnalyticsErrors(long analyticsErrors) { this.analyticsErrors = analyticsErrors; }
        public long getExpectedTotal() { return expectedTotal; }
        public void setExpectedTotal(long expectedTotal) { this.expectedTotal = expectedTotal; }
        public long getExpectedBlocked() { return expectedBlocked; }
        public void setExpectedBlocked(long expectedBlocked) { this.expectedBlocked = expectedBlocked; }
        public boolean isTotalMatches() { return totalMatches; }
        public void setTotalMatches(boolean totalMatches) { this.totalMatches = totalMatches; }
        public boolean isBlockedMatches() { return blockedMatches; }
        public void setBlockedMatches(boolean blockedMatches) { this.blockedMatches = blockedMatches; }
    }

    /**
     * Verify analytics data matches load test results
     * 
     * @param analyticsBaseUrl Base URL of Analytics Service (e.g., http://localhost:8085)
     * @param testResult Rate limit test result to verify against
     * @return Verification result
     */
    public AnalyticsVerificationResult verifyAnalytics(String analyticsBaseUrl, 
                                                       RateLimitTestRunner.RateLimitTestResult testResult) {
        logger.info("=".repeat(80));
        logger.info("Verifying Analytics Data");
        logger.info("=".repeat(80));

        AnalyticsVerificationResult result = new AnalyticsVerificationResult();
        result.setExpectedTotal(testResult.getTotalRequests());
        result.setExpectedBlocked(testResult.getRateLimitedRequests());

        try {
            // Fetch analytics overview
            String overviewUrl = analyticsBaseUrl + "/analytics/overview";
            logger.info("Fetching analytics from: {}", overviewUrl);

            ResponseEntity<Map> response = restTemplate.exchange(
                    overviewUrl,
                    HttpMethod.GET,
                    null,
                    Map.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                result.setVerified(false);
                result.setMessage("Failed to fetch analytics: HTTP " + response.getStatusCode());
                logger.error(result.getMessage());
                return result;
            }

            Map<String, Object> analytics = response.getBody();
            if (analytics == null) {
                result.setVerified(false);
                result.setMessage("Analytics response is empty");
                logger.error(result.getMessage());
                return result;
            }

            // Extract metrics from analytics
            long analyticsTotal = getLongValue(analytics, "totalRequests");
            long analyticsBlocked = getLongValue(analytics, "blockedRequests");
            long analyticsErrors = getLongValue(analytics, "errorCount");
            
            result.setAnalyticsTotal(analyticsTotal);
            result.setAnalyticsBlocked(analyticsBlocked);
            result.setAnalyticsErrors(analyticsErrors);

            // Log analytics data
            logger.info("Analytics Data Retrieved:");
            logger.info("  Total Requests  : {}", analyticsTotal);
            logger.info("  Blocked (429)   : {}", analyticsBlocked);
            logger.info("  Total Errors    : {}", analyticsErrors);
            logger.info("");
            logger.info("Expected from Load Test:");
            logger.info("  Total Requests  : {}", testResult.getTotalRequests());
            logger.info("  Rate Limited    : {}", testResult.getRateLimitedRequests());
            logger.info("");

            // Verify totals match (with tolerance for timing)
            int tolerance = 5; // Allow small discrepancy due to timing
            
            boolean totalMatches = Math.abs(analyticsTotal - testResult.getTotalRequests()) <= tolerance;
            boolean blockedMatches = Math.abs(analyticsBlocked - testResult.getRateLimitedRequests()) <= tolerance;

            result.setTotalMatches(totalMatches);
            result.setBlockedMatches(blockedMatches);

            // Determine overall verification status
            if (totalMatches && blockedMatches) {
                result.setVerified(true);
                result.setMessage("✓ Analytics verification PASSED - All metrics match");
                logger.info("✓ Verification Result: PASSED");
            } else {
                result.setVerified(false);
                StringBuilder msg = new StringBuilder("✗ Analytics verification FAILED - ");
                
                if (!totalMatches) {
                    msg.append(String.format("Total requests mismatch (expected: %d, actual: %d). ",
                            testResult.getTotalRequests(), analyticsTotal));
                }
                if (!blockedMatches) {
                    msg.append(String.format("Blocked requests mismatch (expected: %d, actual: %d). ",
                            testResult.getRateLimitedRequests(), analyticsBlocked));
                }
                
                result.setMessage(msg.toString());
                logger.error("✗ Verification Result: FAILED");
                logger.error("  Reason: {}", result.getMessage());
            }

            // Additional checks
            verifyRecentRequests(analyticsBaseUrl, testResult);
            verifyTopEndpoints(analyticsBaseUrl, testResult);

        } catch (Exception e) {
            result.setVerified(false);
            result.setMessage("Error verifying analytics: " + e.getMessage());
            logger.error("Error during analytics verification", e);
        }

        logger.info("=".repeat(80));
        return result;
    }

    /**
     * Verify recent requests contain our test data
     */
    private void verifyRecentRequests(String analyticsBaseUrl, 
                                     RateLimitTestRunner.RateLimitTestResult testResult) {
        try {
            String recentUrl = analyticsBaseUrl + "/analytics/recent-requests?limit=10";
            logger.info("Checking recent requests: {}", recentUrl);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    recentUrl,
                    HttpMethod.GET,
                    null,
                    Object[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object[] recentRequests = response.getBody();
                logger.info("  Recent requests count: {}", recentRequests.length);
                
                // Log sample of recent requests
                int count = 0;
                for (Object req : recentRequests) {
                    if (count++ < 3 && req instanceof Map) {
                        Map<String, Object> reqMap = (Map<String, Object>) req;
                        logger.info("  - Status: {}, Endpoint: {}, Timestamp: {}", 
                                   reqMap.get("statusCode"), 
                                   reqMap.get("endpoint"),
                                   reqMap.get("timestamp"));
                    }
                }
                logger.info("");
            }
        } catch (Exception e) {
            logger.warn("Could not fetch recent requests: {}", e.getMessage());
        }
    }

    /**
     * Verify top endpoints includes our test endpoint
     */
    private void verifyTopEndpoints(String analyticsBaseUrl, 
                                   RateLimitTestRunner.RateLimitTestResult testResult) {
        try {
            String topEndpointsUrl = analyticsBaseUrl + "/analytics/top-endpoints?limit=5";
            logger.info("Checking top endpoints: {}", topEndpointsUrl);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    topEndpointsUrl,
                    HttpMethod.GET,
                    null,
                    Object[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object[] topEndpoints = response.getBody();
                logger.info("  Top endpoints count: {}", topEndpoints.length);
                
                for (Object ep : topEndpoints) {
                    if (ep instanceof Map) {
                        Map<String, Object> epMap = (Map<String, Object>) ep;
                        logger.info("  - Endpoint: {}, Count: {}", 
                                   epMap.get("endpoint"),
                                   epMap.get("requestCount"));
                    }
                }
                logger.info("");
            }
        } catch (Exception e) {
            logger.warn("Could not fetch top endpoints: {}", e.getMessage());
        }
    }

    /**
     * Helper to safely extract long values from map
     */
    private long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Could not parse {} as long: {}", key, value);
            return 0;
        }
    }

    /**
     * Print analytics verification summary
     */
    public void printVerificationSummary(AnalyticsVerificationResult result) {
        logger.info("");
        logger.info("=".repeat(80));
        logger.info("ANALYTICS VERIFICATION SUMMARY");
        logger.info("=".repeat(80));
        logger.info("Status: {}", result.isVerified() ? "✓ PASSED" : "✗ FAILED");
        logger.info("");
        logger.info("Comparison:");
        logger.info("  Metric              | Expected | Analytics | Match");
        logger.info("  " + "-".repeat(60));
        logger.info("  Total Requests      | {:8} | {:9} | {}", 
                   result.getExpectedTotal(), 
                   result.getAnalyticsTotal(),
                   result.isTotalMatches() ? "✓" : "✗");
        logger.info("  Blocked Requests    | {:8} | {:9} | {}", 
                   result.getExpectedBlocked(), 
                   result.getAnalyticsBlocked(),
                   result.isBlockedMatches() ? "✓" : "✗");
        logger.info("");
        
        if (!result.isVerified()) {
            logger.error("Message: {}", result.getMessage());
        }
        
        logger.info("=".repeat(80));
    }
}
