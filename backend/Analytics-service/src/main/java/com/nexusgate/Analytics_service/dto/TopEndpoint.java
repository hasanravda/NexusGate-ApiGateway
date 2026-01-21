package com.nexusgate.Analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for top endpoint statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopEndpoint {
    private String path;
    private Long serviceRouteId;
    private Long requestCount;
    private Double avgLatencyMs;
    private Long errorCount;
}
