package com.nexusgate.config_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveRateLimitResponse {

    private Long rateLimitId;           // Which rate limit rule was used
    private Long apiKeyId;
    private Long serviceRouteId;
    private Integer requestsPerMinute;
    private Integer requestsPerHour;
    private Integer requestsPerDay;
    private String source;              // "SPECIFIC", "DEFAULT", "GLOBAL", "SYSTEM"
    private boolean isActive;

    // Helper method to create system default
    public static EffectiveRateLimitResponse systemDefault(Long apiKeyId, Long serviceRouteId) {
        return EffectiveRateLimitResponse.builder()
                .apiKeyId(apiKeyId)
                .serviceRouteId(serviceRouteId)
                .requestsPerMinute(1000)
                .requestsPerHour(50000)
                .requestsPerDay(1000000)
                .source("SYSTEM")
                .isActive(true)
                .build();
    }
}