package com.nexusgate.config_service.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitDto {

    private Long id;

    private Long apiKeyId;  // ✅ NULLABLE!

    private Long serviceRouteId;  // ✅ NEW! NULLABLE!

    private Integer requestsPerMinute;
    private Integer requestsPerHour;
    private Integer requestsPerDay;

    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String notes;
}

