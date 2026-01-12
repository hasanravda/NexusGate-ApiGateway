package com.nexusgate.config_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// ========== RESPONSE DTO ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRouteDto {
    
    private Long id;
    private String serviceName;
    private String serviceDescription;
    private String publicPath;
    private String targetUrl;
    private String allowedMethods;
    private Integer rateLimitPerMinute;
    private Integer rateLimitPerHour;
    private Boolean isActive;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String notes;
}