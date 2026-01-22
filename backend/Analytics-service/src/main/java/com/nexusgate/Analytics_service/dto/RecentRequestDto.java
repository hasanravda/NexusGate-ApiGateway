package com.nexusgate.Analytics_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for recent request response with optional enriched fields
 * Maintains backward compatibility while adding apiKeyName and serviceName
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecentRequestDto {
    
    // Original fields (always present)
    private Long id;
    private Long apiKeyId;
    private Long serviceRouteId;
    private String method;
    private String path;
    private Integer status;
    private Long latencyMs;
    private String clientIp;
    private Boolean rateLimited;
    private Instant timestamp;
    
    // New optional enriched fields (null if lookup fails)
    private String apiKeyName;
    private String serviceName;
}
