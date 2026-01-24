package com.nexusgate.Analytics_service.controller;

import com.nexusgate.Analytics_service.dto.DashboardMetricsResponse;
import com.nexusgate.Analytics_service.dto.RateLimitViolationResponse;
import com.nexusgate.Analytics_service.service.MetricsAggregationService;
import com.nexusgate.Analytics_service.service.ViolationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * DashboardAnalyticsController
 * 
 * READ-ONLY API for dashboard consumption
 * Provides aggregated metrics and queries for Logs & Violations dashboard
 * 
 * All endpoints are GET requests and do not modify data
 * Optimized for dashboard visualization and reporting
 * 
 * CACHING STRATEGY (FUTURE):
 * - Cache frequently accessed metrics in Redis (1-5 min TTL)
 * - Use ETags for conditional requests
 * - Implement HTTP cache headers
 */
@Slf4j
@RestController
@RequestMapping("/analytics/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardAnalyticsController {

    private final ViolationService violationService;
    private final MetricsAggregationService metricsAggregationService;

    /**
     * GET /analytics/dashboard/metrics
     * 
     * Get all dashboard summary metrics in one call
     * Optimized for dashboard loading - single API call for all metrics
     * 
     * Returns:
     * - Total violations today
     * - Total blocked requests
     * - Average latency (24h)
     * - Total requests (24h)
     * - Success rate
     * 
     * @return Dashboard metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics() {
        log.debug("Dashboard metrics requested");
        
        DashboardMetricsResponse metrics = metricsAggregationService.getDashboardMetrics();
        
        // Add violations today to the response
        Long violationsToday = violationService.getViolationCountToday();
        metrics.setViolationsToday(violationsToday);
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /analytics/dashboard/violations/today/count
     * 
     * Get rate limit violations count for today
     * Dashboard requirement: violations today card
     * 
     * @return Count of today's violations
     */
    @GetMapping("/violations/today/count")
    public ResponseEntity<Map<String, Long>> getViolationsCountToday() {
        log.debug("Violations count (today) requested");
        
        Long count = violationService.getViolationCountToday();
        
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * GET /analytics/dashboard/requests/blocked/count
     * 
     * Get total blocked requests count
     * Dashboard requirement: blocked requests card
     * 
     * Blocked requests are those that failed authentication/authorization
     * 
     * @return Count of blocked requests
     */
    @GetMapping("/requests/blocked/count")
    public ResponseEntity<Map<String, Long>> getBlockedRequestsCount() {
        log.debug("Blocked requests count requested");
        
        Long count = metricsAggregationService.getBlockedRequestsCount();
        
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * GET /analytics/dashboard/latency/average
     * 
     * Get average response time for last 24 hours
     * Dashboard requirement: average latency card
     * 
     * @return Average latency in milliseconds
     */
    @GetMapping("/latency/average")
    public ResponseEntity<Map<String, Object>> getAverageLatency() {
        log.debug("Average latency requested");
        
        Double avgLatency = metricsAggregationService.getAverageLatency();
        
        return ResponseEntity.ok(Map.of(
            "averageLatencyMs", avgLatency,
            "period", "24h"
        ));
    }

    /**
     * GET /analytics/dashboard/violations/recent
     * 
     * Get recent rate limit violations with pagination
     * Dashboard requirement: violations table
     * 
     * Default limit: 10
     * Max limit: 100
     * 
     * @param limit Number of violations to return
     * @param page Page number (0-based)
     * @return Page of recent violations
     */
    @GetMapping("/violations/recent")
    public ResponseEntity<Page<RateLimitViolationResponse>> getRecentViolations(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int page) {
        
        log.debug("Recent violations requested: limit={}, page={}", limit, page);
        
        // Enforce max limit
        int safeLimit = Math.min(limit, 100);
        
        Pageable pageable = PageRequest.of(page, safeLimit);
        Page<RateLimitViolationResponse> violations = violationService.getRecentViolations(pageable);
        
        return ResponseEntity.ok(violations);
    }

    /**
     * GET /analytics/dashboard/violations/range
     * 
     * Get violations in specific time range
     * Useful for custom date filtering on dashboard
     * 
     * @param startTime Start timestamp (ISO-8601 or epoch millis)
     * @param endTime End timestamp (ISO-8601 or epoch millis)
     * @param limit Number of results
     * @param page Page number
     * @return Page of violations
     */
    @GetMapping("/violations/range")
    public ResponseEntity<Page<RateLimitViolationResponse>> getViolationsInRange(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int page) {
        
        // Default to last 7 days if not specified
        Instant start = startTime != null 
                ? Instant.parse(startTime) 
                : Instant.now().minus(7, ChronoUnit.DAYS);
        
        Instant end = endTime != null 
                ? Instant.parse(endTime) 
                : Instant.now();
        
        log.debug("Violations in range: {} to {}", start, end);
        
        int safeLimit = Math.min(limit, 100);
        Pageable pageable = PageRequest.of(page, safeLimit);
        
        Page<RateLimitViolationResponse> violations = 
                violationService.getViolationsBetween(start, end, pageable);
        
        return ResponseEntity.ok(violations);
    }

    /**
     * Health check for dashboard APIs
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "dashboard-analytics"
        ));
    }
}
