package com.nexusgate.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRouteResponse {
    private Long id;
    private String publicPath;
    private String targetUrl;
    private List<String> allowedMethods;
    private Boolean authRequired;
    private String authType;
    private Boolean rateLimitEnabled;
    private Integer rateLimitPerMinute;
    private Integer rateLimitPerHour;
    private Integer timeoutMs;
    private String customHeaders;
    private Boolean isActive;
}
