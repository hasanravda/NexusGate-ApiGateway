package com.nexusgate.Analytics_service.controller;

import com.nexusgate.Analytics_service.dto.LogEventRequest;
import com.nexusgate.Analytics_service.service.LogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * LogController
 * 
 * Receives analytics events from the Gateway (fire-and-forget)
 * Returns 202 Accepted immediately
 */
@Slf4j
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    /**
     * POST /logs
     * 
     * Receive analytics event from Gateway
     * - Validates request
     * - Returns 202 Accepted immediately
     * - Processing happens asynchronously
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> receiveLog(@Valid @RequestBody LogEventRequest request) {
        log.debug("Received log event: serviceRouteId={}, status={}", 
                request.getServiceRouteId(), request.getStatus());

        // Process log asynchronously (non-blocking)
        logService.processLogEvent(request);

        // Return 202 Accepted immediately
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Log event accepted"));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
