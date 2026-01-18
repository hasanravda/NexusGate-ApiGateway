package com.nexusgate.loadtest.model;

import com.nexusgate.loadtest.dto.LoadTestRequest;
import com.nexusgate.loadtest.dto.LoadTestStatus;
import com.nexusgate.loadtest.service.MetricsCollector;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents an active load test execution instance.
 * Maintains test state, configuration, and references to running tasks.
 */
public class TestExecution {

    private final String testId;
    private final LoadTestRequest request;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private volatile LoadTestStatus status;
    private final AtomicBoolean stopRequested;
    
    // Metrics tracking
    private final MetricsCollector metricsCollector;
    
    // Concurrent task management
    private final List<CompletableFuture<Void>> clientFutures;

    public TestExecution(String testId, LoadTestRequest request) {
        this.testId = testId;
        this.request = request;
        this.startTime = LocalDateTime.now();
        this.status = LoadTestStatus.RUNNING;
        this.stopRequested = new AtomicBoolean(false);
        this.metricsCollector = new MetricsCollector();
        this.clientFutures = new CopyOnWriteArrayList<>();
    }

    // Getters
    public String getTestId() {
        return testId;
    }

    public LoadTestRequest getRequest() {
        return request;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LoadTestStatus getStatus() {
        return status;
    }

    public void setStatus(LoadTestStatus status) {
        this.status = status;
    }

    public AtomicBoolean getStopRequested() {
        return stopRequested;
    }

    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    public List<CompletableFuture<Void>> getClientFutures() {
        return clientFutures;
    }

    /**
     * Adds a client future to track concurrent execution.
     */
    public void addClientFuture(CompletableFuture<Void> future) {
        clientFutures.add(future);
    }

    /**
     * Requests graceful stop of the test execution.
     */
    public void requestStop() {
        stopRequested.set(true);
    }

    /**
     * Checks if stop has been requested.
     */
    public boolean isStopRequested() {
        return stopRequested.get();
    }
}
