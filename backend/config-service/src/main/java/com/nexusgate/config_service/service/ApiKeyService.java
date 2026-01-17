package com.nexusgate.config_service.service;

import com.nexusgate.config_service.dto.ApiKeyDto;
import com.nexusgate.config_service.dto.CreateApiKeyRequest;
import com.nexusgate.config_service.exception.ResourceNotFoundException;
import com.nexusgate.config_service.model.ApiKey;
import com.nexusgate.config_service.repository.ApiKeyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<ApiKeyDto> getAllApiKeys(){
        return  apiKeyRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ApiKeyDto> getApiKeysByUserId(Long userId) {
        return apiKeyRepository.findByCreatedByUserId(userId)  // CHANGED
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ApiKeyDto getApiKeyById(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found with id: " + id));
        return convertToDto(apiKey);
    }

    public ApiKeyDto validateApiKey(String keyValue) {
        ApiKey apiKey = apiKeyRepository.findByKeyValue(keyValue)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid API key"));

        if (!apiKey.getIsActive()) {
            throw new ResourceNotFoundException("API key is inactive");
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("API key has expired");
        }

        // Update last used timestamp
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        return convertToDto(apiKey);
    }

    @Transactional
    public void revokeApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found with id: " + id));

        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
        log.info("Revoked API key: {} for client: {}", apiKey.getKeyName(), apiKey.getClientName());
    }

    @Transactional
    public ApiKeyDto updateApiKey(Long id, CreateApiKeyRequest request) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found with id: " + id));

        // Update fields (except keyValue which should never change)
        apiKey.setKeyName(request.getKeyName());
        apiKey.setClientName(request.getClientName());
        apiKey.setClientEmail(request.getClientEmail());
        apiKey.setClientCompany(request.getClientCompany());
        apiKey.setExpiresAt(request.getExpiresAt());
        apiKey.setNotes(request.getNotes());

        apiKey = apiKeyRepository.save(apiKey);
        log.info("Updated API key: {} for client: {}", apiKey.getKeyName(), apiKey.getClientName());

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
