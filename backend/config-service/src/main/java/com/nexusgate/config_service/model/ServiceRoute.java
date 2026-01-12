package com.nexusgate.config_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ServiceRoute - YOUR backend APIs that you're exposing through the gateway
 *
 * STAGE 1 (Now): Register your local services
 * STAGE 2 (AWS): Gateway routes external traffic to these services
 */
@Entity
@Table(name = "service_routes", indexes = {
        @Index(name = "idx_public_path", columnList = "public_path")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;  // "user-service", "order-service"

    @Column(name = "service_description", length = 500)
    private String serviceDescription;

    // ============ ROUTING CONFIG ============

    /**
     * Public path pattern that external clients will call
     * Examples: "/api/users/**", "/api/orders/**"
     */
    @Column(name = "public_path", nullable = false, unique = true, length = 200)
    private String publicPath;

    /**
     * Your actual backend service URL (local or internal)
     * Examples:
     * - "http://localhost:8082/users"
     * - "http://user-service:8082" (Docker)
     */
    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    /**
     * Allowed HTTP methods (comma-separated)
     * Example: "GET,POST,PUT,DELETE"
     */
    @Column(name = "allowed_methods", length = 100)
    @Builder.Default
    private String allowedMethods = "GET,POST,PUT,DELETE";

    // ============ AUTHENTICATION CONFIG (NEW!) ============

    /**
     * Does this API require authentication?
     * true → Client must provide valid API key or JWT
     * false → Publicly accessible, no auth needed
     */
    @Column(name = "auth_required")
    @Builder.Default
    private Boolean authRequired = true;

    /**
     * Which auth type is required?
     * Options: "API_KEY", "JWT", "BOTH", "NONE"
     */
    @Column(name = "auth_type", length = 20)
    @Builder.Default
    private String authType = "API_KEY";  // Default to API key

    // ============ RATE LIMITING CONFIG ============

    /**
     * Is rate limiting enabled for this API?
     * If false, no rate limits apply (be careful!)
     */
    @Column(name = "rate_limit_enabled")
    @Builder.Default
    private Boolean rateLimitEnabled = true;

    /**
     * Default rate limits for this API
     * These apply if no custom rate limit exists for an API key
     */
    @Column(name = "rate_limit_per_minute")
    @Builder.Default
    private Integer rateLimitPerMinute = 100;

    @Column(name = "rate_limit_per_hour")
    @Builder.Default
    private Integer rateLimitPerHour = 5000;

    // ============ TIMEOUT CONFIG ============

    /**
     * Request timeout in milliseconds
     * Gateway will wait this long for backend response
     */
    @Column(name = "timeout_ms")
    @Builder.Default
    private Integer timeoutMs = 30000;  // Default 30 seconds

    // ============ CUSTOM HEADERS ============

    /**
     * Custom headers to add to requests (JSON format)
     * Example: {"X-Service-Version": "1.0", "X-Internal-Token": "secret"}
     */
    @Column(name = "custom_headers", columnDefinition = "TEXT")
    private String customHeaders;  // Store as JSON string

    // ============ STATUS & METADATA ============

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "notes", length = 500)
    private String notes;
}

/**
 * EXAMPLE CONFIGURATIONS:
 *
 * 1. PUBLIC API (No auth, no rate limits):
 *    serviceName: "public-data-service"
 *    publicPath: "/api/public/**"
 *    targetUrl: "http://localhost:8082/public"
 *    authRequired: false
 *    rateLimitEnabled: false
 *
 * 2. PROTECTED API (API key required, rate limited):
 *    serviceName: "user-service"
 *    publicPath: "/api/users/**"
 *    targetUrl: "http://localhost:8083/users"
 *    authRequired: true
 *    authType: "API_KEY"
 *    rateLimitEnabled: true
 *    defaultRateLimitPerMinute: 200
 *
 * 3. JWT-ONLY API (For admin dashboard):
 *    serviceName: "admin-service"
 *    publicPath: "/api/admin/**"
 *    targetUrl: "http://localhost:8084/admin"
 *    authRequired: true
 *    authType: "JWT"
 *    rateLimitEnabled: false
 */