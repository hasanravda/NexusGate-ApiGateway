package com.nexusgate.Analytics_service.service;

import com.nexusgate.Analytics_service.dto.LogEventRequest;
import com.nexusgate.Analytics_service.model.RequestLog;
import com.nexusgate.Analytics_service.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * LogService
 * 
 * Responsible for:
 * 1. Persisting request logs to PostgreSQL
 * 2. Delegating to MetricsService to update Prometheus metrics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final RequestLogRepository requestLogRepository;
    private final MetricsService metricsService;

    /**
     * Process incoming log event
     * - Save to database
     * - Update metrics
     * - Fire-and-forget (non-blocking for Gateway)
     */
    @Transactional
    public void processLogEvent(LogEventRequest request) {
        try {
            // Parse timestamp
            Instant timestamp = Instant.parse(request.getTimestamp());

            // Build and save RequestLog entity
            RequestLog requestLog = RequestLog.builder()
                    .apiKeyId(request.getApiKeyId())
                    .serviceRouteId(request.getServiceRouteId())
                    .method(request.getMethod())
                    .path(request.getPath())
                    .status(request.getStatus())
                    .latencyMs(request.getLatencyMs())
                    .clientIp(request.getClientIp())
                    .rateLimited(request.getRateLimited())
                    .timestamp(timestamp)
                    .build();

            requestLogRepository.save(requestLog);

            // Update Micrometer metrics
            metricsService.recordRequest(
                    request.getServiceRouteId(),
                    request.getStatus(),
                    request.getLatencyMs(),
                    request.getRateLimited()
            );

            log.debug("Processed log event: serviceRouteId={}, status={}, latencyMs={}",
                    request.getServiceRouteId(), request.getStatus(), request.getLatencyMs());

        } catch (Exception e) {
            log.error("Failed to process log event: {}", e.getMessage(), e);
            // Do not throw exception - analytics should not break Gateway
        }
    }
}
