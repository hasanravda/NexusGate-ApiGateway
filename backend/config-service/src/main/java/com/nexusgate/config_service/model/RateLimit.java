package com.nexusgate.config_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * RateLimit - Defines rate limits PER API KEY PER SERVICE ROUTE
 *
 * Examples:
 * - API Key "nx_lendingkart_xxx" on "/api/users" → 200 req/min
 * - API Key "nx_lendingkart_xxx" on "/api/payments" → 50 req/min
 * - API Key "nx_fintech_yyy" on "/api/users" → 100 req/min
 */
@Entity
@Table(name = "rate_limits",
        indexes = {
                @Index(name = "idx_api_key_id", columnList = "api_key_id"),
                @Index(name = "idx_service_route_id", columnList = "service_route_id"),
                @Index(name = "idx_api_key_service", columnList = "api_key_id,service_route_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_api_key_service", columnNames = {"api_key_id", "service_route_id"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Which API key does this rate limit apply to?
     * If NULL → applies to all API keys as DEFAULT for this service route
     */
    
    @Column(name = "api_key_id", nullable = false, unique = true)
    private Long apiKeyId;  // Foreign key to api_keys table

    /**
     * Which service route does this rate limit apply to?
     * If NULL → applies globally to this API key across all routes
     */
    @Column(name = "service_route_id")
    private Long serviceRouteId;

    // Rate limits
    @Column(name = "requests_per_minute", nullable = false)
    private Integer requestsPerMinute;

    @Column(name = "requests_per_hour")
    private Integer requestsPerHour;

    @Column(name = "requests_per_day")
    private Integer requestsPerDay;

    @Column(name = "is_active")
    private Boolean isActive = true;

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
 * HOW IT WORKS:
 *
 * 1. DEFAULT RATE LIMIT FOR A SERVICE ROUTE:
 *    apiKeyId = NULL, serviceRouteId = 1 (user-service)
 *    → All API keys get 100 req/min on /api/users
 *
 * 2. CUSTOM RATE LIMIT FOR SPECIFIC API KEY ON SPECIFIC ROUTE:
 *    apiKeyId = 5 (LendingKart), serviceRouteId = 1 (user-service)
 *    → LendingKart gets 200 req/min on /api/users
 *
 * 3. GLOBAL RATE LIMIT FOR AN API KEY:
 *    apiKeyId = 5 (LendingKart), serviceRouteId = NULL
 *    → LendingKart gets 500 req/min across ALL routes combined
 *
 * PRIORITY (when checking limits):
 * 1. Check: apiKeyId + serviceRouteId (most specific)
 * 2. Check: serviceRouteId only (default for route)
 * 3. Check: apiKeyId only (global for key)
 * 4. Use system default (1000 req/min)
 */