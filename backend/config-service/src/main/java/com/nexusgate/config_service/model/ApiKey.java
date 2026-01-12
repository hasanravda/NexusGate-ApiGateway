package com.nexusgate.config_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_key_value", columnList = "key_value"),
        @Index(name = "idx_created_by_user_id", columnList = "created_by_user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The actual API key that client uses
    @Column(name = "key_value", nullable = false, unique = true, length = 64)
    private String keyValue;  // nx_lendingkart_prod_abc123

    // Human-readable name for internal reference
    @Column(name = "key_name", nullable = false)
    private String keyName;  // "LendingKart Production Key"

    // === Client Info (just metadata/labels for reference) ===
    @Column(name = "client_name")
    private String clientName;  // "LendingKart"

    @Column(name = "client_email")
    private String clientEmail;  // "dev@lendingkart.com"

    @Column(name = "client_company")
    private String clientCompany;  // "LendingKart Technologies"

    // === Which employee created this key ===
    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;  // Foreign key to users table

    // === Status ===
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "notes", length = 500)
    private String notes;  // Any additional notes
}
