package com.nexusgate.config_service.controller;

import com.nexusgate.config_service.dto.ApiKeyDto;
import com.nexusgate.config_service.dto.CreateApiKeyRequest;
import com.nexusgate.config_service.model.ApiKey;
import com.nexusgate.config_service.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
