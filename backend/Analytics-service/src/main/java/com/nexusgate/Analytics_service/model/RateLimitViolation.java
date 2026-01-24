package com.nexusgate.Analytics_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * RateLimitViolation Entity
 * Stores rate limit violation events for monitoring and analysis
 * 
 * These violations are pushed by the Gateway when a rate limit is exceeded
 * and are used for dashboard analytics and alerting
 */
@Entity
@Table(name = "rate_limit_violations", indexes = {
        @Index(name = "idx_violation_timestamp", columnList = "timestamp"),
        @Index(name = "idx_violation_api_key", columnList = "apiKey"),
        @Index(name = "idx_violation_service", columnList = "serviceName")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * API key that violated the rate limit
     */
    @Column(nullable = false, length = 100)
    private String apiKey;

    /**
     * Service name that was being accessed
     */
    @Column(nullable = false, length = 100)
    private String serviceName;

    /**
     * Specific endpoint that was rate limited
     */
    @Column(nullable = false, length = 500)
    private String endpoint;

    /**
     * HTTP method (GET, POST, etc.)
     */
    @Column(nullable = false, length = 10)
    private String httpMethod;

    /**
     * Rate limit that was exceeded (e.g., "100/min", "1000/hour")
     */
    @Column(nullable = false, length = 50)
    private String limitValue;

    /**
     * Actual number of requests that triggered the violation
     */
    @Column(nullable = false)
    private Long actualValue;

    /**
     * Client IP address
     */
    @Column(length = 45)
    private String clientIp;

    /**
     * When the violation occurred
     */
    @Column(nullable = false)
    private Instant timestamp;

    /**
     * Additional metadata (optional JSON)
     * Can store info like: user agent, request ID, etc.
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
