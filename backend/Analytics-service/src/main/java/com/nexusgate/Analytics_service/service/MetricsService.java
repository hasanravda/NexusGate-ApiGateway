package com.nexusgate.Analytics_service.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * MetricsService
 * 
 * Responsible for updating Micrometer metrics that are exposed via Prometheus.
 * Does NOT calculate rates or percentiles - Prometheus does that.
 */
@Slf4j
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> rateLimitCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> latencyTimers = new ConcurrentHashMap<>();

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record request metrics
     */
    public void recordRequest(Long serviceRouteId, Integer status, Long latencyMs, Boolean rateLimited) {
        String routeTag = String.valueOf(serviceRouteId);

        // Increment total requests counter
        getRequestCounter(routeTag).increment();

        // Increment error counter if status >= 400
        if (status >= 400) {
            getErrorCounter(routeTag).increment();
        }

        // Increment rate limit counter if rateLimited
        if (rateLimited) {
            getRateLimitCounter(routeTag).increment();
        }

        // Record latency
        getLatencyTimer(routeTag).record(latencyMs, TimeUnit.MILLISECONDS);

        log.debug("Recorded metrics for serviceRouteId={}, status={}, latencyMs={}, rateLimited={}",
                serviceRouteId, status, latencyMs, rateLimited);
    }

    /**
     * Get or create request counter for a service route
     */
    private Counter getRequestCounter(String serviceRouteId) {
        return requestCounters.computeIfAbsent(serviceRouteId, id ->
                Counter.builder("nexus_requests_total")
                        .description("Total number of requests")
                        .tag("serviceRouteId", id)
                        .register(meterRegistry)
        );
    }

    /**
     * Get or create error counter for a service route
     */
    private Counter getErrorCounter(String serviceRouteId) {
        return errorCounters.computeIfAbsent(serviceRouteId, id ->
                Counter.builder("nexus_errors_total")
                        .description("Total number of errors (status >= 400)")
                        .tag("serviceRouteId", id)
                        .register(meterRegistry)
        );
    }

    /**
     * Get or create rate limit counter for a service route
     */
    private Counter getRateLimitCounter(String serviceRouteId) {
        return rateLimitCounters.computeIfAbsent(serviceRouteId, id ->
                Counter.builder("nexus_rate_limit_violations_total")
                        .description("Total number of rate limit violations")
                        .tag("serviceRouteId", id)
                        .register(meterRegistry)
        );
    }

    /**
     * Get or create latency timer for a service route
     */
    private Timer getLatencyTimer(String serviceRouteId) {
        return latencyTimers.computeIfAbsent(serviceRouteId, id ->
                Timer.builder("nexus_request_latency")
                        .description("Request latency in milliseconds")
                        .tag("serviceRouteId", id)
                        .register(meterRegistry)
        );
    }
}
