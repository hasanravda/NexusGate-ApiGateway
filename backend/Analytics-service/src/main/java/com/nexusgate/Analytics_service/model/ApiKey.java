package com.nexusgate.Analytics_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * ApiKey Entity (read-only for lookups)
 * Used to enrich analytics responses with API key names
 */
@Entity
@Table(name = "api_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_value", nullable = false, unique = true, length = 64)
    private String keyValue;

    @Column(name = "key_name", nullable = false, length = 255)
    private String keyName;

    @Column(name = "client_name", length = 255)
    private String clientName;

    @Column(name = "client_email", length = 255)
    private String clientEmail;

    @Column(name = "client_company", length = 255)
    private String clientCompany;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
