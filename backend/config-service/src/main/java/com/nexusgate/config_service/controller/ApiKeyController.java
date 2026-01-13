package com.nexusgate.config_service.controller;

import com.nexusgate.config_service.dto.ApiKeyDto;
import com.nexusgate.config_service.dto.CreateApiKeyRequest;
import com.nexusgate.config_service.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiKeyDto> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        ApiKeyDto apiKey= apiKeyService.createApiKey(request);
        return new ResponseEntity<>(apiKey, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyDto>> getAllApiKeys() {
        List<ApiKeyDto> apiKeys = apiKeyService.getAllApiKeys();
        return ResponseEntity.ok(apiKeys);
    }

    // get api key by creator user id
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ApiKeyDto>> getApiKeysByUserId(@PathVariable Long userId){
        List<ApiKeyDto> apiKey = apiKeyService.getApiKeysByUserId(userId);
        return new ResponseEntity<>(apiKey, HttpStatus.OK);
    }


    // get apikey by id
    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyDto> getApiKeyById(@PathVariable Long id) {
        ApiKeyDto apiKey = apiKeyService.getApiKeyById(id);
        return ResponseEntity.ok(apiKey);
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiKeyDto> validateApiKey(@RequestParam String keyValue) {
        ApiKeyDto apiKey = apiKeyService.validateApiKey(keyValue);
        return ResponseEntity.ok(apiKey);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiKeyDto> updateApiKey(
            @PathVariable Long id,
            @Valid @RequestBody CreateApiKeyRequest request) {
        ApiKeyDto apiKey = apiKeyService.updateApiKey(id, request);
        return ResponseEntity.ok(apiKey);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable Long id) {
        apiKeyService.revokeApiKey(id);
        return ResponseEntity.noContent().build();
    }
}
