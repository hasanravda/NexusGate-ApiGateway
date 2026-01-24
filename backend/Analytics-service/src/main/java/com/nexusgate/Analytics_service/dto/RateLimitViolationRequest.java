package com.nexusgate.Analytics_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * RateLimitViolationRequest
 * 
 * DTO for ingesting rate limit violation events from Gateway
 * This is the contract between Gateway and Analytics Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitViolationRequest {

    @NotBlank(message = "API key is required")
    private String apiKey;

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @NotBlank(message = "Endpoint is required")
    private String endpoint;

    @NotBlank(message = "HTTP method is required")
    private String httpMethod;

    @NotBlank(message = "Limit value is required")
    private String limitValue;  // e.g., "100/min"

    @NotNull(message = "Actual value is required")
    @Positive(message = "Actual value must be positive")
    private Long actualValue;

    private String clientIp;

    private String metadata;  // Optional JSON metadata

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;
}
