package com.nexusgate.Analytics_service.service;

import com.nexusgate.Analytics_service.dto.RequestLogRequest;
import com.nexusgate.Analytics_service.model.RequestLog;
import com.nexusgate.Analytics_service.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LogIngestService
 * 
 * Handles ingestion of request logs from Gateway
 * Responsible for persisting log events for analytics
 * 
 * FUTURE ENHANCEMENTS:
 * - Kafka consumer for async ingestion at scale
 * - Batch processing for high throughput
 * - Data retention policies (archive old logs)
 * - Log enrichment (geo-location, user agent parsing)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogIngestService {

    private final RequestLogRepository requestLogRepository;

    /**
     * Ingest request log event from Gateway
     * 
     * This method is called by Gateway after each request is processed
     * Stores complete request/response data for analytics
     * 
     * @param request Request log details
     * @return Saved log ID
     */
    @Transactional
    public Long ingestRequestLog(RequestLogRequest request) {
        log.debug("Ingesting request log: method={}, path={}, status={}", 
                request.getMethod(), request.getPath(), request.getStatus());

        // Convert DTO to entity
        RequestLog log = RequestLog.builder()
                .apiKeyId(request.getApiKeyId())
                .serviceRouteId(request.getServiceRouteId())
                .method(request.getMethod())
                .path(request.getPath())
                .status(request.getStatus())
                .latencyMs(request.getLatencyMs())
                .clientIp(request.getClientIp())
                .rateLimited(request.getRateLimited())
                .blocked(request.getBlocked() != null ? request.getBlocked() : false)
                .timestamp(request.getTimestamp())
                .build();

        RequestLog saved = requestLogRepository.save(log);
        
        // FUTURE: Async processing
        // - Publish to Kafka topic for real-time stream processing
        // - Trigger alerts if error rate spikes
        // - Update cached metrics in Redis
        
        return saved.getId();
    }

    /**
     * Batch ingest multiple logs
     * FUTURE: For high-throughput scenarios
     * 
     * @param requests Batch of log requests
     */
    @Transactional
    public void ingestBatch(Iterable<RequestLogRequest> requests) {
        // FUTURE: Implement batch processing for better performance
        log.info("Batch ingestion not yet implemented");
    }
}
