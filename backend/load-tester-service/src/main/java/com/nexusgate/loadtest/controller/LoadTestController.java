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
import java.util.Map;

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
        logger.info("Received load test request - Endpoint: {}, Rate: {} req/s, Duration: {}s",
                request.getTargetEndpoint(), request.getRequestRate(), request.getDurationSeconds());

        try {
            String testId = loadTestService.startLoadTest(request);

            Map<String, Object> response = new HashMap<>();
            response.put("testId", testId);
            response.put("status", "RUNNING");
            response.put("message", "Load test started successfully");

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            logger.error("Failed to start load test", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to start load test");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
