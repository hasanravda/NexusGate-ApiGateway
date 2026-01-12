package com.nexusgate.config_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rate_limits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key_id", nullable = false, unique = true)
    private Long apiKeyId;  // Foreign key to api_keys table

    @Column(name = "requests_per_minute", nullable = false)
    private Integer requestsPerMinute;

    @Column(name = "requests_per_hour")
    private Integer requestsPerHour;

    @Column(name = "requests_per_day")
    private Integer requestsPerDay;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}