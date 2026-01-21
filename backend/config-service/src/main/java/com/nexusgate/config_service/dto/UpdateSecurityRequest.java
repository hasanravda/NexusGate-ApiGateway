package com.nexusgate.config_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ========== SECURITY UPDATE REQUEST DTO ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSecurityRequest {
    
    private Boolean requiresApiKey;  // Required: true or false
}
