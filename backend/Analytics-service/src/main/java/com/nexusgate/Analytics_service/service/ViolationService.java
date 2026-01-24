package com.nexusgate.Analytics_service.service;

import com.nexusgate.Analytics_service.dto.RateLimitViolationRequest;
import com.nexusgate.Analytics_service.dto.RateLimitViolationResponse;
import com.nexusgate.Analytics_service.model.RateLimitViolation;
import com.nexusgate.Analytics_service.repository.RateLimitViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * ViolationService
 * 
 * Handles rate limit violation tracking and querying
 * This service is responsible for:
 * - Ingesting violation events from Gateway
 * - Providing violation data for dashboard
 * - Aggregating violation statistics
 * 
 * FUTURE: Can be extended with:
 * - Kafka for async ingestion
 * - Redis caching for hot queries
 * - Alerting when violations exceed threshold
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViolationService {

    private final RateLimitViolationRepository violationRepository;

    /**
     * Ingest violation event from Gateway
     * Called when a rate limit is exceeded
     * 
     * @param request Violation details
     * @return Saved violation ID
     */
    @Transactional
    public String ingestViolation(RateLimitViolationRequest request) {
        log.info("Ingesting rate limit violation: apiKey={}, service={}, endpoint={}", 
                request.getApiKey(), request.getServiceName(), request.getEndpoint());

        RateLimitViolation violation = RateLimitViolation.builder()
                .apiKey(request.getApiKey())
                .serviceName(request.getServiceName())
                .endpoint(request.getEndpoint())
                .httpMethod(request.getHttpMethod())
                .limitValue(request.getLimitValue())
                .actualValue(request.getActualValue())
                .clientIp(request.getClientIp())
                .timestamp(request.getTimestamp())
                .metadata(request.getMetadata())
                .build();

        RateLimitViolation saved = violationRepository.save(violation);
        
        log.debug("Violation saved with ID: {}", saved.getId());
        
        // FUTURE: Trigger alert if violations exceed threshold
        // FUTURE: Publish to Kafka topic for real-time processing
        
        return saved.getId().toString();
    }

    /**
     * Get violations count for a specific date
     * Used by dashboard for daily trends
     * 
     * @param date Date to query
     * @return Count of violations
     */
    @Transactional(readOnly = true)
    public Long getViolationCountByDate(LocalDate date) {
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        
        Long count = violationRepository.countViolationsByDate(startOfDay, endOfDay);
        
        log.debug("Violations on {}: {}", date, count);
        
        return count;
    }

    /**
     * Get violations count for today
     * Dashboard requirement
     * 
     * @return Count of today's violations
     */
    @Transactional(readOnly = true)
    public Long getViolationCountToday() {
        return getViolationCountByDate(LocalDate.now(ZoneOffset.UTC));
    }

    /**
     * Get recent violations with pagination
     * Dashboard requirement
     * 
     * @param pageable Pagination parameters
     * @return Page of violations
     */
    @Transactional(readOnly = true)
    public Page<RateLimitViolationResponse> getRecentViolations(Pageable pageable) {
        Page<RateLimitViolation> violations = violationRepository.findAllByOrderByTimestampDesc(pageable);
        
        return violations.map(this::toResponse);
    }

    /**
     * Get violations in time range
     * 
     * @param start Start time
     * @param end End time
     * @param pageable Pagination
     * @return Page of violations
     */
    @Transactional(readOnly = true)
    public Page<RateLimitViolationResponse> getViolationsBetween(Instant start, Instant end, Pageable pageable) {
        Page<RateLimitViolation> violations = violationRepository
                .findByTimestampBetweenOrderByTimestampDesc(start, end, pageable);
        
        return violations.map(this::toResponse);
    }

    /**
     * Convert entity to response DTO
     */
    private RateLimitViolationResponse toResponse(RateLimitViolation violation) {
        return RateLimitViolationResponse.builder()
                .id(violation.getId().toString())
                .apiKey(violation.getApiKey())
                .serviceName(violation.getServiceName())
                .endpoint(violation.getEndpoint())
                .httpMethod(violation.getHttpMethod())
                .limitValue(violation.getLimitValue())
                .actualValue(violation.getActualValue())
                .clientIp(violation.getClientIp())
                .timestamp(violation.getTimestamp())
                .build();
    }
}
