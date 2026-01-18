package com.nexusgate.loadtest.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe metrics collector for load test execution.
 * Uses atomic operations and concurrent data structures to safely
 * collect metrics from multiple concurrent clients.
 */
@Component
public class MetricsCollector {

    // Request counters (thread-safe)
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder rateLimitedRequests = new LongAdder();
    private final LongAdder errorRequests = new LongAdder();

    // Latency tracking (thread-safe)
    private final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyMs = new AtomicLong(0);

    // HTTP status tracking
    private final ConcurrentHashMap<Integer, LongAdder> statusCodeCounts = new ConcurrentHashMap<>();

    /**
     * Records the result of a single HTTP request.
     * This method is called concurrently from multiple client threads.
     *
     * @param statusCode HTTP status code received
     * @param latencyMs Request latency in milliseconds
     */
    public void recordRequest(int statusCode, long latencyMs) {
        // Increment total request counter
        totalRequests.increment();

        // Categorize by status code
        if (statusCode >= 200 && statusCode < 300) {
            successfulRequests.increment();
        } else if (statusCode == 429) {
            rateLimitedRequests.increment();
        } else {
            errorRequests.increment();
        }

        // Track status code distribution
        statusCodeCounts.computeIfAbsent(statusCode, k -> new LongAdder()).increment();

        // Record latency metrics
        latencies.add(latencyMs);
        totalLatencyMs.addAndGet(latencyMs);

        // Update min/max latency (atomic compare-and-swap)
        updateMin(minLatencyMs, latencyMs);
        updateMax(maxLatencyMs, latencyMs);
    }

    /**
     * Records a failed request (timeout, connection error, etc.)
     */
    public void recordError(long latencyMs) {
        totalRequests.increment();
        errorRequests.increment();
        latencies.add(latencyMs);
        totalLatencyMs.addAndGet(latencyMs);
    }

    // Getters for metrics

    public long getTotalRequests() {
        return totalRequests.sum();
    }

    public long getSuccessfulRequests() {
        return successfulRequests.sum();
    }

    public long getRateLimitedRequests() {
        return rateLimitedRequests.sum();
    }

    public long getErrorRequests() {
        return errorRequests.sum();
    }

    /**
     * Calculates average latency across all requests.
     */
    public double getAverageLatencyMs() {
        long total = totalRequests.sum();
        if (total == 0) return 0.0;
        return (double) totalLatencyMs.get() / total;
    }

    /**
     * Calculates 95th percentile latency.
     * Sorts latency list and returns value at 95% position.
     */
    public long getP95LatencyMs() {
        if (latencies.isEmpty()) return 0;

        // Create a copy to avoid concurrent modification during sort
        List<Long> sortedLatencies = new ArrayList<>(latencies);
        Collections.sort(sortedLatencies);

        int p95Index = (int) Math.ceil(sortedLatencies.size() * 0.95) - 1;
        p95Index = Math.max(0, Math.min(p95Index, sortedLatencies.size() - 1));

        return sortedLatencies.get(p95Index);
    }

    public long getMinLatencyMs() {
        long min = minLatencyMs.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxLatencyMs() {
        return maxLatencyMs.get();
    }

    public ConcurrentHashMap<Integer, LongAdder> getStatusCodeCounts() {
        return statusCodeCounts;
    }

    /**
     * Atomically updates minimum value using compare-and-swap.
     */
    private void updateMin(AtomicLong current, long newValue) {
        long currentMin;
        do {
            currentMin = current.get();
            if (newValue >= currentMin) return;
        } while (!current.compareAndSet(currentMin, newValue));
    }

    /**
     * Atomically updates maximum value using compare-and-swap.
     */
    private void updateMax(AtomicLong current, long newValue) {
        long currentMax;
        do {
            currentMax = current.get();
            if (newValue <= currentMax) return;
        } while (!current.compareAndSet(currentMax, newValue));
    }

    /**
     * Resets all metrics (useful for test cleanup).
     */
    public void reset() {
        totalRequests.reset();
        successfulRequests.reset();
        rateLimitedRequests.reset();
        errorRequests.reset();
        latencies.clear();
        totalLatencyMs.set(0);
        minLatencyMs.set(Long.MAX_VALUE);
        maxLatencyMs.set(0);
        statusCodeCounts.clear();
    }
}
