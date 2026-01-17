package com.nexusgate.config_service.service;

import com.nexusgate.config_service.dto.CreateRateLimitRequest;
import com.nexusgate.config_service.dto.EffectiveRateLimitResponse;
import com.nexusgate.config_service.dto.RateLimitDto;
import com.nexusgate.config_service.exception.ResourceNotFoundException;
import com.nexusgate.config_service.model.RateLimit;
import com.nexusgate.config_service.repository.RateLimitRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitRepository rateLimitRepository;

    @Transactional
    public RateLimitDto createRateLimit(@Valid CreateRateLimitRequest request) {

        // Check if rate limit already exists for this combination
        if (rateLimitRepository.existsByApiKeyIdAndServiceRouteId(
                request.getApiKeyId(), request.getServiceRouteId())) {
            throw new IllegalArgumentException(
                    "Rate limit already exists for this API key and service route combination");
        }

        RateLimit rateLimit = RateLimit.builder()
                .apiKeyId(request.getApiKeyId())
                .serviceRouteId(request.getServiceRouteId())
                .requestsPerMinute(request.getRequestsPerMinute())
                .requestsPerHour(request.getRequestsPerHour())
                .requestsPerDay(request.getRequestsPerDay())
                .isActive(true)
                .notes(request.getNotes())
                .build();

        RateLimit saved = rateLimitRepository.save(rateLimit);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RateLimitDto> getAllRateLimits() {
        return rateLimitRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RateLimitDto getRateLimitById(Long id) {
        RateLimit rateLimit = rateLimitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rate limit not found with id: " + id));
        return toDto(rateLimit);
    }

    @Transactional(readOnly = true)
    public List<RateLimitDto> getRateLimitsByApiKey(Long apiKeyId) {
        return rateLimitRepository.findByApiKeyId(apiKeyId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RateLimitDto> getRateLimitsByServiceRoute(Long serviceRouteId) {
        return rateLimitRepository.findByServiceRouteId(serviceRouteId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get effective rate limit for API key + service route combination
     *
     * Priority order:
     * 1. Specific: apiKeyId + serviceRouteId (most specific)
     * 2. Default: serviceRouteId only (default for all keys)
     * 3. Global: apiKeyId only (global for this key)
     * 4. System: 1000 req/min default
     */
    @Transactional(readOnly = true)
    public EffectiveRateLimitResponse getEffectiveRateLimit(Long apiKeyId, Long serviceRouteId) {

        // 1. Check for specific rate limit (API key + service route)
        Optional<RateLimit> specific = rateLimitRepository.findByApiKeyIdAndServiceRouteId(
                apiKeyId, serviceRouteId);
        if (specific.isPresent()) {
            return toEffectiveResponse(specific.get(), "SPECIFIC");
        }

        // 2. Check for default rate limit (service route only)
        Optional<RateLimit> defaultLimit = rateLimitRepository.findByApiKeyIdIsNullAndServiceRouteId(
                serviceRouteId);
        if (defaultLimit.isPresent()) {
            return toEffectiveResponse(defaultLimit.get(), "DEFAULT");
        }

        // 3. Check for global rate limit (API key only)
        Optional<RateLimit> global = rateLimitRepository.findByApiKeyIdAndServiceRouteIdIsNull(
                apiKeyId);
        if (global.isPresent()) {
            return toEffectiveResponse(global.get(), "GLOBAL");
        }

        // 4. Return system default
        return EffectiveRateLimitResponse.systemDefault(apiKeyId, serviceRouteId);
    }

    @Transactional
    public RateLimitDto updateRateLimit(Long id, CreateRateLimitRequest request) {
        RateLimit existing = rateLimitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rate limit not found with id: " + id));

        existing.setRequestsPerMinute(request.getRequestsPerMinute());
        existing.setRequestsPerHour(request.getRequestsPerHour());
        existing.setRequestsPerDay(request.getRequestsPerDay());
        existing.setNotes(request.getNotes());

        RateLimit updated = rateLimitRepository.save(existing);
        return toDto(updated);
    }

    @Transactional
    public RateLimitDto toggleActiveStatus(Long id) {
        RateLimit rateLimit = rateLimitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rate limit not found with id: " + id));

        rateLimit.setIsActive(!rateLimit.getIsActive());
        RateLimit updated = rateLimitRepository.save(rateLimit);
        return toDto(updated);
    }

    @Transactional
    public void deleteRateLimit(Long id) {
        if (!rateLimitRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rate limit not found with id: " + id);
        }
        rateLimitRepository.deleteById(id);
    }

    // Helper: Convert entity to DTO
    private RateLimitDto toDto(RateLimit rateLimit) {
        return RateLimitDto.builder()
                .id(rateLimit.getId())
                .apiKeyId(rateLimit.getApiKeyId())
                .serviceRouteId(rateLimit.getServiceRouteId())
                .requestsPerMinute(rateLimit.getRequestsPerMinute())
                .requestsPerHour(rateLimit.getRequestsPerHour())
                .requestsPerDay(rateLimit.getRequestsPerDay())
                .isActive(rateLimit.getIsActive())
                .createdAt(rateLimit.getCreatedAt())
                .updatedAt(rateLimit.getUpdatedAt())
                .notes(rateLimit.getNotes())
                .build();
    }

    // Helper: Convert entity to effective response
    private EffectiveRateLimitResponse toEffectiveResponse(RateLimit rateLimit, String source) {
        return EffectiveRateLimitResponse.builder()
                .rateLimitId(rateLimit.getId())
                .apiKeyId(rateLimit.getApiKeyId())
                .serviceRouteId(rateLimit.getServiceRouteId())
                .requestsPerMinute(rateLimit.getRequestsPerMinute())
                .requestsPerHour(rateLimit.getRequestsPerHour())
                .requestsPerDay(rateLimit.getRequestsPerDay())
                .source(source)
                .isActive(rateLimit.getIsActive())
                .build();
    }
}