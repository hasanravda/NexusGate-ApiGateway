package com.nexusgate.Analytics_service.service;

import com.nexusgate.Analytics_service.dto.DashboardMetricsResponse;
import com.nexusgate.Analytics_service.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * MetricsAggregationService
 * 
 * Provides aggregated metrics for dashboard consumption
 * Handles complex queries and calculations for analytics
 * 
 * FUTURE ENHANCEMENTS:
 * - Redis caching for frequently accessed metrics (TTL: 1-5 minutes)
 * - Pre-computed aggregations for better performance
 * - Time-series data for trending
 * - Percentile calculations (p50, p95, p99)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsAggregationService {

    private final RequestLogRepository requestLogRepository;

    /**
     * Get dashboard summary metrics
     * Dashboard requirement: provides all key metrics in one call
     * 
     * PERFORMANCE NOTE:
     * - Current implementation runs multiple queries
     * - FUTURE: Cache in Redis with 1-minute TTL
     * - FUTURE: Use materialized views for better performance
     * 
     * @return Dashboard metrics
     */
    @Transactional(readOnly = true)
    public DashboardMetricsResponse getDashboardMetrics() {
        log.debug("Computing dashboard metrics");

        // Calculate time boundaries
        Instant now = Instant.now();
        Instant last24Hours = now.minus(24, ChronoUnit.HOURS);

        // Query all metrics
        Long blockedCount = requestLogRepository.countBlockedRequests();
        Double avgLatency = requestLogRepository.averageLatencyLast24Hours(last24Hours);
        Long totalRequests = requestLogRepository.countRequestsBetween(last24Hours, now);
        Long errors = requestLogRepository.countErrorsBetween(last24Hours, now);

        // Calculate success rate
        Double successRate = totalRequests > 0 
                ? ((totalRequests - errors) * 100.0 / totalRequests) 
                : 100.0;

        log.debug("Dashboard metrics: blocked={}, avgLatency={}, totalRequests={}, successRate={}", 
                blockedCount, avgLatency, totalRequests, successRate);

        return DashboardMetricsResponse.builder()
                .blockedRequests(blockedCount)
                .averageLatencyMs(avgLatency != null ? avgLatency : 0.0)
                .totalRequests(totalRequests)
                .successRate(Math.round(successRate * 100.0) / 100.0)  // Round to 2 decimals
                .build();
    }

    /**
     * Get blocked requests count
     * Dashboard requirement
     * 
     * @return Count of blocked requests
     */
    @Transactional(readOnly = true)
    public Long getBlockedRequestsCount() {
        Long count = requestLogRepository.countBlockedRequests();
        log.debug("Total blocked requests: {}", count);
        
        // FUTURE: Cache in Redis with key "metrics:blocked:count"
        
        return count;
    }

    /**
     * Get average latency for last 24 hours
     * Dashboard requirement
     * 
     * @return Average latency in milliseconds
     */
    @Transactional(readOnly = true)
    public Double getAverageLatency() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        Double avgLatency = requestLogRepository.averageLatencyLast24Hours(since);
        
        log.debug("Average latency (24h): {} ms", avgLatency);
        
        // FUTURE: Cache in Redis with key "metrics:latency:avg:24h"
        
        return avgLatency != null ? avgLatency : 0.0;
    }

    /**
     * Get requests count for specific time range
     * 
     * @param start Start time
     * @param end End time
     * @return Count of requests
     */
    @Transactional(readOnly = true)
    public Long getRequestsCountBetween(Instant start, Instant end) {
        Long count = requestLogRepository.countRequestsBetween(start, end);
        log.debug("Requests between {} and {}: {}", start, end, count);
        return count;
    }

    /**
     * Get success rate for time range
     * 
     * @param start Start time
     * @param end End time
     * @return Success rate percentage
     */
    @Transactional(readOnly = true)
    public Double getSuccessRate(Instant start, Instant end) {
        Long total = requestLogRepository.countRequestsBetween(start, end);
        Long errors = requestLogRepository.countErrorsBetween(start, end);
        
        if (total == 0) {
            return 100.0;
        }
        
        Double successRate = ((total - errors) * 100.0 / total);
        return Math.round(successRate * 100.0) / 100.0;
    }
}
