package com.nexusgate.Analytics_service.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for incoming analytics events from the Gateway
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEventRequest {

    @NotNull(message = "apiKeyId is required")
    @Positive(message = "apiKeyId must be positive")
    private Long apiKeyId;

    @NotNull(message = "serviceRouteId is required")
    @Positive(message = "serviceRouteId must be positive")
    private Long serviceRouteId;

    @NotBlank(message = "method is required")
    @Size(max = 10, message = "method must not exceed 10 characters")
    private String method;

    @NotBlank(message = "path is required")
    @Size(max = 500, message = "path must not exceed 500 characters")
    private String path;

    @NotNull(message = "status is required")
    @Min(value = 100, message = "status must be a valid HTTP status code")
    @Max(value = 599, message = "status must be a valid HTTP status code")
    private Integer status;

    @NotNull(message = "latencyMs is required")
    @PositiveOrZero(message = "latencyMs must be non-negative")
    private Long latencyMs;

    @NotNull(message = "rateLimited is required")
    private Boolean rateLimited;

    @NotBlank(message = "clientIp is required")
    @Size(max = 45, message = "clientIp must not exceed 45 characters")
    private String clientIp;

    @NotBlank(message = "timestamp is required")
    private String timestamp; // ISO 8601 format
}
