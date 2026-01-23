package com.nexusgate.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveRateLimitResponse {
    private Integer requestsPerMinute;
    private Integer requestsPerHour;
    
    @JsonProperty("active")  // Maps to "active" in JSON response
    private Boolean isActive;
}