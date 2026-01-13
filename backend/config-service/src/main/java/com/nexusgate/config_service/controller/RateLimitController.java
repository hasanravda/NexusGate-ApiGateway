package com.nexusgate.config_service.controller;

import com.nexusgate.config_service.dto.CreateRateLimitRequest;
import com.nexusgate.config_service.dto.EffectiveRateLimitResponse;
import com.nexusgate.config_service.dto.RateLimitDto;
import com.nexusgate.config_service.service.RateLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rate-limits")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    /**
     * Create new rate limit
     * POST /rate-limits
     */
    @PostMapping
    public ResponseEntity<RateLimitDto> createRateLimit(@Valid @RequestBody CreateRateLimitRequest request) {
        RateLimitDto rateLimit = rateLimitService.createRateLimit(request);
        return new ResponseEntity<>(rateLimit, HttpStatus.CREATED);
    }

    /**
     * Get all rate limits
     * GET /rate-limits
     */
    @GetMapping
    public ResponseEntity<List<RateLimitDto>> getAllRateLimits() {
        List<RateLimitDto> rateLimits = rateLimitService.getAllRateLimits();
        return ResponseEntity.ok(rateLimits);
    }

    /**
     * Get rate limit by ID
     * GET /rate-limits/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<RateLimitDto> getRateLimitById(@PathVariable Long id) {
        RateLimitDto rateLimit = rateLimitService.getRateLimitById(id);
        return ResponseEntity.ok(rateLimit);
    }

    /**
     * Get all rate limits for a specific API key
     * GET /rate-limits/by-api-key/{apiKeyId}
     */
    @GetMapping("/by-api-key/{apiKeyId}")
    public ResponseEntity<List<RateLimitDto>> getRateLimitsByApiKey(@PathVariable Long apiKeyId) {
        List<RateLimitDto> rateLimits = rateLimitService.getRateLimitsByApiKey(apiKeyId);
        return ResponseEntity.ok(rateLimits);
    }

    /**
     * Get all rate limits for a specific service route
     * GET /rate-limits/by-service-route/{serviceRouteId}
     */
    @GetMapping("/by-service-route/{serviceRouteId}")
    public ResponseEntity<List<RateLimitDto>> getRateLimitsByServiceRoute(@PathVariable Long serviceRouteId) {
        List<RateLimitDto> rateLimits = rateLimitService.getRateLimitsByServiceRoute(serviceRouteId);
        return ResponseEntity.ok(rateLimits);
    }

    /**
     * Get effective rate limit for API key + service route combination
     * CRITICAL FOR GATEWAY!
     *
     * GET /rate-limits/check?apiKeyId=1&serviceRouteId=2
     *
     * This checks in priority order:
     * 1. Specific: apiKeyId + serviceRouteId
     * 2. Default: serviceRouteId only (applies to all keys)
     * 3. Global: apiKeyId only (applies to all routes)
     * 4. System default: 1000 req/min
     */
    @GetMapping("/check")
    public ResponseEntity<EffectiveRateLimitResponse> getEffectiveRateLimit(
            @RequestParam Long apiKeyId,
            @RequestParam Long serviceRouteId) {

        EffectiveRateLimitResponse rateLimit = rateLimitService.getEffectiveRateLimit(apiKeyId, serviceRouteId);
        return ResponseEntity.ok(rateLimit);
    }

    /**
     * Update rate limit
     * PUT /rate-limits/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<RateLimitDto> updateRateLimit(
            @PathVariable Long id,
            @Valid @RequestBody CreateRateLimitRequest request) {

        RateLimitDto updated = rateLimitService.updateRateLimit(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete rate limit
     * DELETE /rate-limits/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRateLimit(@PathVariable Long id) {
        rateLimitService.deleteRateLimit(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle active status
     * PATCH /rate-limits/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RateLimitDto> toggleActiveStatus(@PathVariable Long id) {
        RateLimitDto updated = rateLimitService.toggleActiveStatus(id);
        return ResponseEntity.ok(updated);
    }
}