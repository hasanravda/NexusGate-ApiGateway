package com.nexusgate.config_service.service;

import com.nexusgate.config_service.dto.ApiKeyDto;
import com.nexusgate.config_service.dto.CreateApiKeyRequest;
import com.nexusgate.config_service.model.ApiKey;
import com.nexusgate.config_service.repository.ApiKeyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j  //Simple Logging Facade for Java
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private static final String KEY_PREFIX = "nx_";

    @Transactional
    public ApiKeyDto createApiKey(CreateApiKeyRequest request){
        String keyValue = generateSecureKey();

        ApiKey apiKey = ApiKey.builder()
                .keyValue(keyValue)
                .keyName(request.getKeyName())
                .clientName(request.getClientName())
                .clientEmail(request.getClientEmail())
                .clientCompany(request.getClientCompany())
                .createdByUserId(request.getCreatedByUserId())
                .isActive(true)
                .expiresAt(request.getExpiresAt())
                .notes(request.getNotes())
                .build();
        apiKey = apiKeyRepository.save(apiKey);

        log.info("Created API key: {} for client: {} by user: {}",
                apiKey.getKeyName(), apiKey.getClientName(), apiKey.getCreatedByUserId());

        return convertToDto(apiKey);
    }


    private String generateSecureKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return KEY_PREFIX + encoded;
    }

    private ApiKeyDto convertToDto(ApiKey apiKey) {
        return ApiKeyDto.builder()
                .id(apiKey.getId())
                .keyValue(apiKey.getKeyValue())
                .keyName(apiKey.getKeyName())
                .clientName(apiKey.getClientName())
                .clientEmail(apiKey.getClientEmail())
                .clientCompany(apiKey.getClientCompany())
                .createdByUserId(apiKey.getCreatedByUserId())
                .isActive(apiKey.getIsActive())
                .expiresAt(apiKey.getExpiresAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .notes(apiKey.getNotes())
                .build();
    }
}
