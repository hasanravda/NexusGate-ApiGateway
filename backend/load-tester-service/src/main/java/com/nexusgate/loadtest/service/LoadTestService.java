package com.nexusgate.loadtest.service;

import com.nexusgate.loadtest.dto.LoadTestRequest;
import com.nexusgate.loadtest.dto.LoadTestResult;
import com.nexusgate.loadtest.dto.LoadTestStatus;
import com.nexusgate.loadtest.model.TestExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Main orchestrator for load testing operations.
 * Manages test lifecycle, spawns concurrent clients, and coordinates execution.
 */
@Service
public class LoadTestService {

    private static final Logger logger = LoggerFactory.getLogger(LoadTestService.class);

    private final HttpClientService httpClientService;
    private final ReportGenerator reportGenerator;
    
    // In-memory storage for active and completed tests
    private final ConcurrentHashMap<String, TestExecution> testExecutions = new ConcurrentHashMap<>();
    
    // Thread pool for concurrent client execution
    private final ExecutorService executorService;

    public LoadTestService(HttpClientService httpClientService, ReportGenerator reportGenerator) {
        this.httpClientService = httpClientService;
        this.reportGenerator = reportGenerator;
        
        // Create a thread pool for concurrent client simulation
        // Using cached thread pool for dynamic concurrency
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Starts a new load test execution asynchronously.
     * Returns immediately with testId and RUNNING status.
     *
     * @param request Load test configuration
     * @return testId for tracking the execution
     */
    public String startLoadTest(LoadTestRequest request) {
        // Generate unique test ID
        String testId = UUID.randomUUID().toString();
        
        // Create test execution instance
        TestExecution execution = new TestExecution(testId, request);
        testExecutions.put(testId, execution);

        logger.info("Starting load test {} - Target: {}, Rate: {} req/s, Duration: {}s, Concurrency: {}",
                testId, request.getTargetEndpoint(), request.getRequestRate(),
                request.getDurationSeconds(), request.getConcurrencyLevel());

        // Execute test asynchronously (non-blocking)
        CompletableFuture.runAsync(() -> executeLoadTest(execution), executorService)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        logger.error("Load test {} failed with error", testId, error);
                        execution.setStatus(LoadTestStatus.FAILED);
                    }
                    finalizeTest(execution);
                });

        return testId;
    }

    /**
     * Core load test execution logic.
     * Spawns concurrent clients and manages request generation.
     */
    private void executeLoadTest(TestExecution execution) {
        LoadTestRequest request = execution.getRequest();
        
        // Calculate per-client request rate
        int totalRequestRate = request.getRequestRate();
        int concurrency = request.getConcurrencyLevel();
        int requestsPerClient = totalRequestRate / concurrency;
        int remainderRequests = totalRequestRate % concurrency;

        logger.info("Test {}: Spawning {} concurrent clients, {} req/s per client",
                execution.getTestId(), concurrency, requestsPerClient);

        // Spawn concurrent clients
        for (int clientId = 0; clientId < concurrency; clientId++) {
            // Distribute remainder requests to first few clients
            final int finalClientId = clientId; // Make effectively final for lambda
            final int clientRequestRate = requestsPerClient + (clientId < remainderRequests ? 1 : 0);
            
            CompletableFuture<Void> clientFuture = CompletableFuture.runAsync(
                    () -> runClient(execution, finalClientId, clientRequestRate),
                    executorService
            );
            
            execution.addClientFuture(clientFuture);
        }

        // Wait for all clients to complete or timeout
        try {
            CompletableFuture<Void> allClients = CompletableFuture.allOf(
                    execution.getClientFutures().toArray(new CompletableFuture[0])
            );
            
            // Wait with timeout
            allClients.get(request.getDurationSeconds() + 10, TimeUnit.SECONDS);
            
            if (execution.isStopRequested()) {
                execution.setStatus(LoadTestStatus.STOPPED);
                logger.info("Test {} stopped by user", execution.getTestId());
            } else {
                execution.setStatus(LoadTestStatus.COMPLETED);
                logger.info("Test {} completed successfully", execution.getTestId());
            }
            
        } catch (TimeoutException e) {
            logger.warn("Test {} timed out", execution.getTestId());
            execution.setStatus(LoadTestStatus.COMPLETED);
        } catch (Exception e) {
            logger.error("Test {} encountered error", execution.getTestId(), e);
            execution.setStatus(LoadTestStatus.FAILED);
        }
    }

    /**
     * Runs a single client that sends requests according to the test pattern.
     */
    private void runClient(TestExecution execution, int clientId, int requestsPerSecond) {
        LoadTestRequest request = execution.getRequest();
        long testDurationMs = request.getDurationSeconds() * 1000L;
        long startTime = System.currentTimeMillis();
        
        // Calculate delay between requests based on pattern
        long delayBetweenRequestsMs = calculateRequestDelay(request.getRequestPattern(), requestsPerSecond);

        logger.debug("Client {} started - {} req/s, delay: {}ms",
                clientId, requestsPerSecond, delayBetweenRequestsMs);

        int requestCount = 0;
        
        while (!execution.isStopRequested() && 
               (System.currentTimeMillis() - startTime) < testDurationMs) {
            
            // Execute single request
            try {
                executeAndRecordRequest(execution);
                requestCount++;
                
                // Rate limiting based on pattern
                if (delayBetweenRequestsMs > 0) {
                    Thread.sleep(delayBetweenRequestsMs);
                }
                
            } catch (InterruptedException e) {
                logger.debug("Client {} interrupted", clientId);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Client {} error executing request", clientId, e);
                execution.getMetricsCollector().recordError(0);
            }
        }

        logger.debug("Client {} finished - sent {} requests", clientId, requestCount);
    }

    /**
     * Executes a single HTTP request and records metrics.
     */
    private void executeAndRecordRequest(TestExecution execution) {
        LoadTestRequest request = execution.getRequest();
        
        httpClientService.executeRequest(
                request.getTargetEndpoint(),
                request.getTargetKey(),
                request.getHttpMethod()
        ).subscribe(
                result -> {
                    // Record successful response
                    execution.getMetricsCollector().recordRequest(
                            result.getStatusCode(),
                            result.getLatencyMs()
                    );
                },
                error -> {
                    // Record error
                    logger.debug("Request failed: {}", error.getMessage());
                    execution.getMetricsCollector().recordError(0);
                }
        );
    }

    /**
     * Calculates delay between requests based on traffic pattern.
     */
    private long calculateRequestDelay(LoadTestRequest.RequestPattern pattern, int requestsPerSecond) {
        if (requestsPerSecond == 0) return 0;
        
        switch (pattern) {
            case CONSTANT_RATE:
                // Evenly distribute requests over time
                return 1000L / requestsPerSecond;
                
            case BURST:
                // Send requests as fast as possible (no delay)
                return 0;
                
            case RAMP_UP:
                // Start slow, gradually increase (simplified: start at half rate)
                return 2000L / requestsPerSecond;
                
            default:
                return 1000L / requestsPerSecond;
        }
    }

    /**
     * Finalizes test execution and sets end time.
     */
    private void finalizeTest(TestExecution execution) {
        execution.setEndTime(LocalDateTime.now());
        logger.info("Test {} finalized - Status: {}, Total Requests: {}",
                execution.getTestId(),
                execution.getStatus(),
                execution.getMetricsCollector().getTotalRequests());
    }

    /**
     * Retrieves current status of a test (running or completed).
     */
    public LoadTestResult getTestStatus(String testId) {
        TestExecution execution = testExecutions.get(testId);
        if (execution == null) {
            throw new IllegalArgumentException("Test not found: " + testId);
        }

        if (execution.getStatus() == LoadTestStatus.RUNNING) {
            return reportGenerator.generatePartialReport(execution);
        } else {
            return reportGenerator.generateReport(execution);
        }
    }

    /**
     * Retrieves final results of a completed test.
     */
    public LoadTestResult getTestResults(String testId) {
        TestExecution execution = testExecutions.get(testId);
        if (execution == null) {
            throw new IllegalArgumentException("Test not found: " + testId);
        }

        return reportGenerator.generateReport(execution);
    }

    /**
     * Stops a running test gracefully.
     */
    public void stopTest(String testId) {
        TestExecution execution = testExecutions.get(testId);
        if (execution == null) {
            throw new IllegalArgumentException("Test not found: " + testId);
        }

        if (execution.getStatus() != LoadTestStatus.RUNNING) {
            throw new IllegalStateException("Test is not running: " + testId);
        }

        logger.info("Stopping test {}", testId);
        execution.requestStop();
    }

    /**
     * Cleanup method for scheduled task to remove old test results.
     * Can be called periodically to prevent memory buildup.
     */
    public void cleanupOldTests(int maxAgeMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        
        testExecutions.entrySet().removeIf(entry -> {
            TestExecution exec = entry.getValue();
            return exec.getEndTime() != null && exec.getEndTime().isBefore(cutoff);
        });
    }
}
