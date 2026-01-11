package com.nexusgate.config_service.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyRequest {

    @NotBlank(message="Key name is required")
    private String keyName;

    private String clientName;

    @Email(message = "Valid email is required")
    private String clientEmail;

    private String clientCompany;

    @NotNull(message = "Created by user ID is required")
    private Long createdByUserId;  // Which Employee created this

    private LocalDateTime expiresAt;
    private String notes;

}
