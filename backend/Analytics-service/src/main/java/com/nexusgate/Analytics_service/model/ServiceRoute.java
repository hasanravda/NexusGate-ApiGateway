package com.nexusgate.Analytics_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * ServiceRoute Entity (read-only for lookups)
 * Used to enrich analytics responses with service names
 */
@Entity
@Table(name = "service_routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "service_description", length = 500)
    private String serviceDescription;

    @Column(name = "public_path", nullable = false, unique = true, length = 200)
    private String publicPath;

    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    @Column(name = "allowed_methods", nullable = false)
    private String[] allowedMethods;

    @Column(name = "auth_required", nullable = false)
    private Boolean authRequired;

    @Column(name = "auth_type", nullable = false, length = 20)
    private String authType;

    @Column(name = "requires_api_key", nullable = false)
    private Boolean requiresApiKey;

    @Column(name = "rate_limit_enabled", nullable = false)
    private Boolean rateLimitEnabled;

    @Column(name = "rate_limit_per_minute")
    private Integer rateLimitPerMinute;

    @Column(name = "rate_limit_per_hour")
    private Integer rateLimitPerHour;

    @Column(name = "timeout_ms")
    private Integer timeoutMs;

    @Column(name = "custom_headers", columnDefinition = "TEXT")
    private String customHeaders;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
