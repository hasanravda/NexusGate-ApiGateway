package com.nexusgate.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveRateLimitResponse {
    private Integer requestsPerMinute;
    private Integer requestsPerHour;
    private Boolean isActive;
}