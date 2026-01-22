package com.nexusgate.loadtest.controller;

import com.nexusgate.loadtest.dto.LoadTestRequest;
import com.nexusgate.loadtest.dto.LoadTestResult;
import com.nexusgate.loadtest.service.LoadTestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for load testing operations.
 * Provides endpoints to start, monitor, and stop load tests.
 */
@RestController
@RequestMapping("/load-test")
public class LoadTestController {

    private static final Logger logger = LoggerFactory.getLogger(LoadTestController.class);

    private final LoadTestService loadTestService;

    public LoadTestController(LoadTestService loadTestService) {
        this.loadTestService = loadTestService;
    }

    /**
     * Starts a new load test execution.
     * Returns immediately with testId and RUNNING status.
     *
     * POST /load-test/start
     * 
     * Example request:
     * {
     *   "targetKey": "nx_lendingkart_prod_abc123",
     *   "targetEndpoint": "http://localhost:8081/api/users",
     *   "requestRate": 100,
     *   "durationSeconds": 30,
     *   "concurrencyLevel": 10,
     *   "requestPattern": "CONSTANT_RATE",
     *   "httpMethod": "GET"
     * }
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startLoadTest(@Valid @RequestBody LoadTestRequest request) {
        logger.info("Received load test request - Endpoint: {}, Rate: {} req/s, Duration: {}s, API Key: {}***",
                request.getTargetEndpoint(), request.getRequestRate(), request.getDurationSeconds(),
                request.getTargetKey() != null && request.getTargetKey().length() > 6 
                    ? request.getTargetKey().substring(0, 6) : "null");

        try {
            // Validate request
            validateLoadTestRequest(request);
            
            String testId = loadTestService.startLoadTest(request);

            Map<String, Object> response = new HashMap<>();
            response.put("testId", testId);
            response.put("status", "RUNNING");
            response.put("message", "Load test started successfully");
            response.put("note", "Monitor the test using GET /load-test/status/" + testId);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid load test request: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid request");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to start load test", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to start load test");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Validates load test request parameters.
     */
    private void validateLoadTestRequest(LoadTestRequest request) {
        if (request.getTargetKey() == null || request.getTargetKey().trim().isEmpty()) {
            throw new IllegalArgumentException("API Key (targetKey) is required and cannot be empty");
        }
        
        if (request.getTargetEndpoint() == null || request.getTargetEndpoint().trim().isEmpty()) {
            throw new IllegalArgumentException("Target endpoint is required and cannot be empty");
        }
        
        if (!request.getTargetEndpoint().startsWith("http://") && !request.getTargetEndpoint().startsWith("https://")) {
            throw new IllegalArgumentException("Target endpoint must start with http:// or https://");
        }
        
        if (request.getRequestRate() <= 0) {
            throw new IllegalArgumentException("Request rate must be greater than 0");
        }
        
        if (request.getDurationSeconds() <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0 seconds");
        }
        
        if (request.getConcurrencyLevel() <= 0) {
            throw new IllegalArgumentException("Concurrency level must be greater than 0");
        }
        
        // Warn about common API key issues
        if (request.getTargetKey().equals("nx_test_key_123")) {
            logger.warn("⚠️  API Key 'nx_test_key_123' is commonly used in examples but may not exist in your database. " +
                       "Valid keys include: nx_lendingkart_prod_abc123, nx_paytm_prod_xyz789, nx_mobikwik_test_def456, nx_test_key_12345");
        }
    }

    /**
     * Retrieves current status of a test execution.
     * Returns partial results if test is still running.
     *
     * GET /load-test/status/{testId}
     */
    @GetMapping("/status/{testId}")
    public ResponseEntity<?> getTestStatus(@PathVariable String testId) {
        logger.debug("Fetching status for test {}", testId);

        try {
            LoadTestResult result = loadTestService.getTestStatus(testId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("Test not found: {}", testId);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Test not found");
            errorResponse.put("testId", testId);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error fetching test status", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch test status");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves final results of a completed test.
     *
     * GET /load-test/result/{testId}
     */
    @GetMapping("/result/{testId}")
    public ResponseEntity<?> getTestResult(@PathVariable String testId) {
        logger.debug("Fetching results for test {}", testId);

        try {
            LoadTestResult result = loadTestService.getTestResults(testId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("Test not found: {}", testId);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Test not found");
            errorResponse.put("testId", testId);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error fetching test results", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch test results");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves final results of a completed test (alternative path).
     *
     * GET /load-test/results/{testId}
     */
    @GetMapping("/results/{testId}")
    public ResponseEntity<?> getTestResults(@PathVariable String testId) {
        logger.debug("Fetching results for test {}", testId);

        try {
            LoadTestResult result = loadTestService.getTestResults(testId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("Test not found: {}", testId);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Test not found");
            errorResponse.put("testId", testId);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error fetching test results", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch test results");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Stops a running test gracefully.
     *
     * DELETE /load-test/stop/{testId}
     */
    @DeleteMapping("/stop/{testId}")
    public ResponseEntity<Map<String, String>> stopTest(@PathVariable String testId) {
        logger.info("Stopping test {}", testId);

        try {
            loadTestService.stopTest(testId);

            Map<String, String> response = new HashMap<>();
            response.put("testId", testId);
            response.put("message", "Test stop requested");
            
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Test not found: {}", testId);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Test not found");
            errorResponse.put("testId", testId);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (IllegalStateException e) {
            logger.warn("Cannot stop test {}: {}", testId, e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Cannot stop test");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error stopping test", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to stop test");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Lists all test executions (running and completed).
     *
     * GET /load-test/list
     */
    @GetMapping("/list")
    public ResponseEntity<?> listAllTests() {
        logger.debug("Fetching all test executions");

        try {
            List<Map<String, Object>> testList = loadTestService.getAllTests()
                    .stream()
                    .map(result -> {
                        Map<String, Object> testInfo = new HashMap<>();
                        testInfo.put("testId", result.getTestId());
                        testInfo.put("status", result.getStatus());
                        testInfo.put("targetEndpoint", result.getTargetEndpoint());
                        testInfo.put("requestRate", result.getConfiguredRequestRate());
                        testInfo.put("totalRequests", result.getTotalRequests());
                        testInfo.put("successRate", result.getSuccessRate());
                        testInfo.put("startTime", result.getStartTime());
                        testInfo.put("endTime", result.getEndTime());
                        return testInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(testList);

        } catch (Exception e) {
            logger.error("Error listing tests", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to list tests");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "load-tester-service");
        return ResponseEntity.ok(response);
    }
}
