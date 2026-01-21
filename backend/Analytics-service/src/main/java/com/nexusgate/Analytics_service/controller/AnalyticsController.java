package com.nexusgate.Analytics_service.controller;

import com.nexusgate.Analytics_service.dto.AnalyticsOverview;
import com.nexusgate.Analytics_service.dto.TopEndpoint;
import com.nexusgate.Analytics_service.model.RequestLog;
import com.nexusgate.Analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AnalyticsController
 * 
 * Provides read-only analytics APIs for dashboards
 * Data comes from PostgreSQL (NOT Prometheus)
 */
@Slf4j
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * GET /analytics/overview
     * 
     * Get analytics overview for the last 24 hours
     */
    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverview> getOverview() {
        log.debug("Fetching analytics overview");
        AnalyticsOverview overview = analyticsService.getOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * GET /analytics/recent-requests
     * 
     * Get recent requests with pagination
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     */
    @GetMapping("/recent-requests")
    public ResponseEntity<Page<RequestLog>> getRecentRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.debug("Fetching recent requests: page={}, size={}", page, size);
        Page<RequestLog> logs = analyticsService.getRecentRequests(page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * GET /analytics/top-endpoints
     * 
     * Get top endpoints by request count
     * 
     * @param limit Number of endpoints to return (default: 10)
     */
    @GetMapping("/top-endpoints")
    public ResponseEntity<List<TopEndpoint>> getTopEndpoints(
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.debug("Fetching top endpoints: limit={}", limit);
        List<TopEndpoint> topEndpoints = analyticsService.getTopEndpoints(limit);
        return ResponseEntity.ok(topEndpoints);
    }
}
