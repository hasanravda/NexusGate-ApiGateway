package com.nexusgate.Analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DashboardMetricsResponse
 * 
 * DTO for dashboard summary metrics
 * Aggregated data for dashboard cards/widgets
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsResponse {

    /**
     * Total rate limit violations today
     */
    private Long violationsToday;

    /**
     * Total blocked requests (all time or in time range)
     */
    private Long blockedRequests;

    /**
     * Average response time in milliseconds (last 24 hours)
     */
    private Double averageLatencyMs;

    /**
     * Total requests in time range
     */
    private Long totalRequests;

    /**
     * Success rate percentage
     */
    private Double successRate;
}
