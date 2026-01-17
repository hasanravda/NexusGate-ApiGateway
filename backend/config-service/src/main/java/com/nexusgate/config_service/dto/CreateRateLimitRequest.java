package com.nexusgate.config_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRateLimitRequest {

    // If null → default rate limit for service route
    private Long apiKeyId;

    // If null → global rate limit for API key
    private Long serviceRouteId;

    @NotNull(message = "Requests per minute is required")
    @Min(value = 1, message = "Requests per minute must be at least 1")
    private Integer requestsPerMinute;

    @Min(value = 1, message = "Requests per hour must be at least 1")
    private Integer requestsPerHour;

    @Min(value = 1, message = "Requests per day must be at least 1")
    private Integer requestsPerDay;

    private String notes;
}