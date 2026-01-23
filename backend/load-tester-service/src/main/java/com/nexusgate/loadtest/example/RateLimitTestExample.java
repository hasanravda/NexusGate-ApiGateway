// ============================================================================
// Example: Rate Limit Testing Usage
// ============================================================================

package com.nexusgate.loadtest.example;

import com.nexusgate.loadtest.tester.AnalyticsVerifier;
import com.nexusgate.loadtest.tester.RateLimitTestRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Example usage of Rate Limit Testing components
 * 
 * This class demonstrates how to:
 * 1. Execute a rate limit test
 * 2. Verify analytics data
 * 3. Handle test results
 */
@Component
public class RateLimitTestExample {

    @Autowired
    private RateLimitTestRunner testRunner;

    @Autowired
    private AnalyticsVerifier analyticsVerifier;

    /**
     * Example 1: Basic Rate Limit Test
     * 
     * Tests that the API Gateway correctly enforces rate limits
     * by sending 50 requests (20 should succeed, 30 should be rate-limited)
     */
    public void example1_BasicRateLimitTest() {
        System.out.println("\n=== Example 1: Basic Rate Limit Test ===\n");

        // Configure test
        RateLimitTestRunner.RateLimitTestConfig config = new RateLimitTestRunner.RateLimitTestConfig(
                "http://localhost:8081/api/users",  // Target endpoint
                "nx_test_key_12345",                 // API key
                50,                                   // Total requests to send
                10,                                   // Concurrent threads
                20                                    // Expected rate limit (requests allowed)
        );

        // Execute test
        RateLimitTestRunner.RateLimitTestResult result = testRunner.runRateLimitTest(config);

        // Check results
        System.out.println("\nTest Results:");
        System.out.println("  Test Passed: " + result.isTestPassed());
        System.out.println("  Total Requests: " + result.getTotalRequests());
        System.out.println("  Successful (2xx): " + result.getSuccessfulRequests());
        System.out.println("  Rate Limited (429): " + result.getRateLimitedRequests());
        System.out.println("  Errors: " + result.getErrorRequests());

        // Verify expected behavior
        if (result.isTestPassed()) {
            System.out.println("\n✓ Rate limiting is working correctly!");
            System.out.println("  - First " + result.getSuccessfulRequests() + " requests succeeded");
            System.out.println("  - Next " + result.getRateLimitedRequests() + " requests were blocked");
        } else {
            System.err.println("\n✗ Rate limit test failed: " + result.getFailureReason());
        }
    }

    /**
     * Example 2: High Concurrency Burst Test
     * 
     * Simulates a traffic spike with high concurrency to ensure
     * rate limiting handles burst traffic correctly
     */
    public void example2_BurstTest() {
        System.out.println("\n=== Example 2: High Concurrency Burst Test ===\n");

        // Configure burst test (no delay between requests)
        RateLimitTestRunner.RateLimitTestConfig config = new RateLimitTestRunner.RateLimitTestConfig(
                "http://localhost:8081/api/users",
                "nx_test_key_12345",
                100,    // High volume
                50,     // High concurrency
                20      // Expected limit
        );
        config.setDelayBetweenRequestsMs(0);  // Send as fast as possible

        // Execute test
        RateLimitTestRunner.RateLimitTestResult result = testRunner.runRateLimitTest(config);

        System.out.println("\nBurst Test Results:");
        System.out.println("  Duration: " + result.getTotalDurationMs() + "ms");
        System.out.println("  Throughput: " + String.format("%.2f", result.getRequestsPerSecond()) + " req/s");
        System.out.println("  Rate Limited: " + result.getRateLimitedRequests() + " requests");

        if (result.getRateLimitedRequests() > 0) {
            System.out.println("\n✓ Rate limiting handled burst traffic correctly");
        }
    }

    /**
     * Example 3: Test with Analytics Verification
     * 
     * Runs a rate limit test and then verifies that the Analytics Service
     * correctly tracked all requests (both allowed and blocked)
     */
    public void example3_TestWithAnalyticsVerification() {
        System.out.println("\n=== Example 3: Test with Analytics Verification ===\n");

        // Step 1: Execute rate limit test
        System.out.println("Step 1: Executing rate limit test...");
        RateLimitTestRunner.RateLimitTestConfig config = new RateLimitTestRunner.RateLimitTestConfig(
                "http://localhost:8081/api/users",
                "nx_test_key_12345",
                50,
                10,
                20
        );

        RateLimitTestRunner.RateLimitTestResult testResult = testRunner.runRateLimitTest(config);

        if (!testResult.isTestPassed()) {
            System.err.println("Test failed, skipping analytics verification");
            return;
        }

        // Step 2: Wait for analytics to process
        System.out.println("\nStep 2: Waiting for analytics to process...");
        try {
            Thread.sleep(3000);  // Wait 3 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Step 3: Verify analytics
        System.out.println("\nStep 3: Verifying analytics...");
        AnalyticsVerifier.AnalyticsVerificationResult analyticsResult = 
                analyticsVerifier.verifyAnalytics("http://localhost:8085", testResult);

        analyticsVerifier.printVerificationSummary(analyticsResult);

        // Check overall status
        if (testResult.isTestPassed() && analyticsResult.isVerified()) {
            System.out.println("\n✓✓ FULL TEST PASSED - Both rate limiting and analytics verified!");
        } else {
            System.err.println("\n✗ Some checks failed:");
            if (!testResult.isTestPassed()) {
                System.err.println("  - Rate limit test: " + testResult.getFailureReason());
            }
            if (!analyticsResult.isVerified()) {
                System.err.println("  - Analytics verification: " + analyticsResult.getMessage());
            }
        }
    }

    /**
     * Example 4: Different Rate Limits for Different API Keys
     * 
     * Tests that different API keys have different rate limits
     */
    public void example4_DifferentRateLimits() {
        System.out.println("\n=== Example 4: Different Rate Limits per API Key ===\n");

        // Test with standard API key (lower limit)
        System.out.println("Testing Standard API Key (limit: 20)...");
        RateLimitTestRunner.RateLimitTestConfig standardConfig = new RateLimitTestRunner.RateLimitTestConfig(
                "http://localhost:8081/api/users",
                "nx_test_key_12345",
                30,
                5,
                20   // Standard limit
        );
        RateLimitTestRunner.RateLimitTestResult standardResult = testRunner.runRateLimitTest(standardConfig);

        // Test with premium API key (higher limit)
        System.out.println("\n\nTesting Premium API Key (limit: 100)...");
        RateLimitTestRunner.RateLimitTestConfig premiumConfig = new RateLimitTestRunner.RateLimitTestConfig(
                "http://localhost:8081/api/users",
                "nx_lendingkart_prod_abc123",  // Premium key
                120,
                10,
                100  // Higher limit for premium
        );
        RateLimitTestRunner.RateLimitTestResult premiumResult = testRunner.runRateLimitTest(premiumConfig);

        // Compare results
        System.out.println("\n\nComparison:");
        System.out.println("  Standard Key:");
        System.out.println("    Successful: " + standardResult.getSuccessfulRequests());
        System.out.println("    Rate Limited: " + standardResult.getRateLimitedRequests());
        System.out.println("  Premium Key:");
        System.out.println("    Successful: " + premiumResult.getSuccessfulRequests());
        System.out.println("    Rate Limited: " + premiumResult.getRateLimitedRequests());

        if (premiumResult.getSuccessfulRequests() > standardResult.getSuccessfulRequests()) {
            System.out.println("\n✓ Premium API key has higher rate limit as expected");
        }
    }

    /**
     * Example 5: Controlled Rate Testing (Gradual Load)
     * 
     * Sends requests with delays to simulate realistic traffic patterns
     */
    public void example5_ControlledRateTest() {
        System.out.println("\n=== Example 5: Controlled Rate Test ===\n");

        // Configure with delays between requests
        RateLimitTestRunner.RateLimitTestConfig config = new RateLimitTestRunner.RateLimitTestConfig(
                "http://localhost:8081/api/users",
                "nx_test_key_12345",
                30,
                5,
                20
        );
        config.setDelayBetweenRequestsMs(100);  // 100ms delay = ~10 req/s

        RateLimitTestRunner.RateLimitTestResult result = testRunner.runRateLimitTest(config);

        System.out.println("\nControlled Rate Results:");
        System.out.println("  Duration: " + result.getTotalDurationMs() + "ms");
        System.out.println("  Avg Throughput: " + String.format("%.2f", result.getRequestsPerSecond()) + " req/s");
        System.out.println("  Successful: " + result.getSuccessfulRequests());
        System.out.println("  Rate Limited: " + result.getRateLimitedRequests());

        System.out.println("\n✓ Controlled rate test demonstrates gradual load behavior");
    }

    /**
     * Example 6: Logging and Request Details
     * 
     * Shows how to access detailed per-request information
     */
    public void example6_DetailedRequestLogging() {
        System.out.println("\n=== Example 6: Detailed Request Logging ===\n");

        RateLimitTestRunner.RateLimitTestConfig config = new RateLimitTestRunner.RateLimitTestConfig(
                "http://localhost:8081/api/users",
                "nx_test_key_12345",
                10,  // Small number for demo
                2,
                5
        );

        RateLimitTestRunner.RateLimitTestResult result = testRunner.runRateLimitTest(config);

        // Access detailed request information
        System.out.println("\nDetailed Request Log:");
        result.getRequestDetails().forEach(detail -> {
            System.out.println(detail.toString());
        });

        System.out.println("\n\nRate Limit Violations:");
        result.getRateLimitViolations().forEach(violation -> {
            System.out.println("  - " + violation);
        });
    }

    /**
     * Main method to run all examples
     */
    public static void main(String[] args) {
        // This would be called from a Spring Boot context
        System.out.println("See individual example methods for usage");
    }
}
