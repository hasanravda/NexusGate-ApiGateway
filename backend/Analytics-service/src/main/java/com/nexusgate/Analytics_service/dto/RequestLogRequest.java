package com.nexusgate.Analytics_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * RequestLogRequest
 * 
 * DTO for ingesting request log events from Gateway
 * Extended to support blocked field for dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLogRequest {

    @NotNull(message = "API key ID is required")
    private Long apiKeyId;

    @NotNull(message = "Service route ID is required")
    private Long serviceRouteId;

    @NotNull(message = "HTTP method is required")
    private String method;

    @NotNull(message = "Request path is required")
    private String path;

    @NotNull(message = "Status code is required")
    private Integer status;

    @NotNull(message = "Latency is required")
    private Long latencyMs;

    @NotNull(message = "Client IP is required")
    private String clientIp;

    @NotNull(message = "Rate limited flag is required")
    private Boolean rateLimited;

    /**
     * Indicates if request was blocked (auth failure, invalid key, etc.)
     */
    @Builder.Default
    private Boolean blocked = false;

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;
}
