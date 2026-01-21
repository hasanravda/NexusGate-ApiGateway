package com.nexusgate.Analytics_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * RequestLog Entity
 * Stores raw request log data received from the Gateway
 */
@Entity
@Table(name = "request_logs", indexes = {
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_service_route", columnList = "serviceRouteId"),
        @Index(name = "idx_api_key", columnList = "apiKeyId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long apiKeyId;

    @Column(nullable = false)
    private Long serviceRouteId;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(nullable = false)
    private Integer status;

    @Column(nullable = false)
    private Long latencyMs;

    @Column(nullable = false, length = 45)
    private String clientIp;

    @Column(nullable = false)
    private Boolean rateLimited;

    @Column(nullable = false)
    private Instant timestamp;
}
