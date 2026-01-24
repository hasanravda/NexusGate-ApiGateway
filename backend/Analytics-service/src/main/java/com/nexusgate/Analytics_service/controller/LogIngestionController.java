package com.nexusgate.Analytics_service.controller;

import com.nexusgate.Analytics_service.dto.RequestLogRequest;
import com.nexusgate.Analytics_service.dto.RateLimitViolationRequest;
import com.nexusgate.Analytics_service.service.LogIngestService;
import com.nexusgate.Analytics_service.service.ViolationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * LogIngestionController
 * 
 * INTERNAL API - Called by Gateway service only
 * Accepts log and violation events for persistence
 * 
 * These endpoints are WRITE-ONLY and used for data ingestion
 * Dashboard queries should use separate read-only endpoints
 * 
 * SECURITY NOTE:
 * In production, these endpoints should be:
 * - Protected with internal API key
 * - Accessible only from Gateway's IP
 * - Or use service mesh mutual TLS
 */
@Slf4j
@RestController
@RequestMapping("/analytics/logs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LogIngestionController {

    private final LogIngestService logIngestService;
    private final ViolationService violationService;

    /**
     * POST /analytics/logs/request
     * 
     * Ingest request log from Gateway
     * Called after each request is processed by Gateway
     * 
     * Returns 202 Accepted for async processing pattern
     * 
     * @param request Request log details
     * @return 202 Accepted with log ID
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> ingestRequestLog(
            @Valid @RequestBody RequestLogRequest request) {
        
        log.debug("Ingesting request log: {} {} - status {}", 
                request.getMethod(), request.getPath(), request.getStatus());

        try {
            Long logId = logIngestService.ingestRequestLog(request);
            
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(Map.of(
                        "message", "Request log accepted",
                        "logId", logId
                    ));
        } catch (Exception e) {
            log.error("Failed to ingest request log", e);
            
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "message", "Failed to ingest log",
                        "error", e.getMessage()
                    ));
        }
    }

    /**
     * POST /analytics/logs/violation
     * 
     * Ingest rate limit violation from Gateway
     * Called when a rate limit is exceeded
     * 
     * Returns 202 Accepted for async processing pattern
     * 
     * @param request Violation details
     * @return 202 Accepted with violation ID
     */
    @PostMapping("/violation")
    public ResponseEntity<Map<String, Object>> ingestViolation(
            @Valid @RequestBody RateLimitViolationRequest request) {
        
        log.info("Ingesting rate limit violation: apiKey={}, service={}, endpoint={}", 
                request.getApiKey(), request.getServiceName(), request.getEndpoint());

        try {
            String violationId = violationService.ingestViolation(request);
            
            // FUTURE: Trigger real-time alerts if violations exceed threshold
            
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(Map.of(
                        "message", "Violation recorded",
                        "violationId", violationId
                    ));
        } catch (Exception e) {
            log.error("Failed to ingest violation", e);
            
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "message", "Failed to record violation",
                        "error", e.getMessage()
                    ));
        }
    }

    /**
     * Health check for ingestion pipeline
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "log-ingestion"
        ));
    }
}
