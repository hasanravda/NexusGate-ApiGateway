package com.nexusgate.Analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for analytics overview response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverview {
    private Long totalRequests;
    private Long totalErrors;
    private Long rateLimitViolations;
    private Double averageLatencyMs;
    private Double errorRate;
}
