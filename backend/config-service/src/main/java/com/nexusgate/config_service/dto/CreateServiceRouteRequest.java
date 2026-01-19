package com.nexusgate.config_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ========== REQUEST DTO ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceRouteRequest {
    
    private String serviceName;          // Required
    private String serviceDescription;   // Optional
    private String publicPath;           // Required: "/api/users/**"
    private String targetUrl;            // Required: "http://user-service:8081"
    private List<String> allowedMethods; // Optional: ["GET","POST","PUT","DELETE"]
    private Integer rateLimitPerMinute;  // Optional: defaults to 100
    private Integer rateLimitPerHour;    // Optional: defaults to 5000
    private Long createdByUserId;        // Required
    private String notes;                // Optional
}

