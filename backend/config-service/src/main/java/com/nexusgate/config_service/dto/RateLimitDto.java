package com.nexusgate.config_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitDto {
    private Long id;

    @NotNull(message = "API key ID is required")
    private Long apiKeyId;  // Always tied to an API key

    @NotNull(message = "Requests per minute is required")
    private Integer requestsPerMinute;

    private Integer requestsPerHour;
    private Integer requestsPerDay;
    private Boolean isActive;
}
