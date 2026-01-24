package com.nexusgate.Analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * RateLimitViolationResponse
 * 
 * DTO for returning violation data to dashboard/clients
 * Simplified view of violation for UI consumption
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitViolationResponse {

    private String id;
    private String apiKey;
    private String serviceName;
    private String endpoint;
    private String httpMethod;
    private String limitValue;
    private Long actualValue;
    private String clientIp;
    private Instant timestamp;
}
