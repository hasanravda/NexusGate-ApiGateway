package com.nexusgate.Analytics_service.service;

import com.nexusgate.Analytics_service.dto.AnalyticsOverview;
import com.nexusgate.Analytics_service.dto.RecentRequestDto;
import com.nexusgate.Analytics_service.dto.TopEndpoint;
import com.nexusgate.Analytics_service.model.ApiKey;
import com.nexusgate.Analytics_service.model.RequestLog;
import com.nexusgate.Analytics_service.model.ServiceRoute;
import com.nexusgate.Analytics_service.repository.ApiKeyRepository;
import com.nexusgate.Analytics_service.repository.RequestLogRepository;
import com.nexusgate.Analytics_service.repository.ServiceRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AnalyticsService
 * 
 * Provides read-only analytics queries from PostgreSQL
 * Used by AnalyticsController to serve dashboard data
 * 
 * All analytics calculations are restricted to the last 24 hours
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final RequestLogRepository requestLogRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ServiceRouteRepository serviceRouteRepository;

    /**
     * Get analytics overview for the last 24 hours
     * 
     * Calculates:
     * - totalRequests: all requests in last 24h
     * - totalErrors: requests with status >= 400
     * - rateLimitViolations: requests where rateLimited == true (no mocks)
     * - averageLatencyMs: average response time
     * - errorRate: percentage of errors
     */
    public AnalyticsOverview getOverview() {
        Instant now = Instant.now();
        Instant yesterday = now.minus(24, ChronoUnit.HOURS);

        Long totalRequests = requestLogRepository.countRequestsBetween(yesterday, now);
        Long totalErrors = requestLogRepository.countErrorsBetween(yesterday, now);
        Long rateLimitViolations = requestLogRepository.countRateLimitViolationsBetween(yesterday, now);
        Double avgLatency = requestLogRepository.averageLatencyBetween(yesterday, now);

        Double errorRate = totalRequests > 0 ? (totalErrors.doubleValue() / totalRequests.doubleValue()) * 100 : 0.0;

        return AnalyticsOverview.builder()
                .totalRequests(totalRequests)
                .totalErrors(totalErrors)
                .rateLimitViolations(rateLimitViolations)
                .averageLatencyMs(avgLatency != null ? avgLatency : 0.0)
                .errorRate(errorRate)
                .build();
    }

    /**
     * Get recent requests (paginated)
     * 
     * Features:
     * - Backend-enforced limit of 20 records max
     * - Always ordered by timestamp DESC (newest first)
     * - Enriched with apiKeyName and serviceName (null if lookup fails)
     * - Defensive null-safe lookups (never throws errors)
     */
    public Page<RecentRequestDto> getRecentRequests(int page, int size) {
        // Backend-enforced limit: max 20 records
        int effectiveSize = Math.min(size, 20);
        
        // Always sort by timestamp DESC
        Pageable pageable = PageRequest.of(page, effectiveSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<RequestLog> logsPage = requestLogRepository.findAll(pageable);
        
        // Enrich with apiKeyName and serviceName
        List<RecentRequestDto> enrichedLogs = logsPage.getContent().stream()
                .map(this::enrichRequestLog)
                .collect(Collectors.toList());
        
        return new PageImpl<>(enrichedLogs, pageable, logsPage.getTotalElements());
    }
    
    /**
     * Enrich a RequestLog with apiKeyName and serviceName
     * Defensive: returns null for optional fields if lookup fails
     */
    private RecentRequestDto enrichRequestLog(RequestLog requestLog) {
        String apiKeyName = null;
        String serviceName = null;
        
        try {
            // Lookup API key name (defensive)
            if (requestLog.getApiKeyId() != null) {
                apiKeyName = apiKeyRepository.findById(requestLog.getApiKeyId())
                        .map(ApiKey::getKeyName)
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("Failed to lookup API key name for ID {}: {}", requestLog.getApiKeyId(), e.getMessage());
        }
        
        try {
            // Lookup service name (defensive)
            if (requestLog.getServiceRouteId() != null) {
                serviceName = serviceRouteRepository.findById(requestLog.getServiceRouteId())
                        .map(ServiceRoute::getServiceName)
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("Failed to lookup service name for ID {}: {}", requestLog.getServiceRouteId(), e.getMessage());
        }
        
        return RecentRequestDto.builder()
                .id(requestLog.getId())
                .apiKeyId(requestLog.getApiKeyId())
                .serviceRouteId(requestLog.getServiceRouteId())
                .method(requestLog.getMethod())
                .path(requestLog.getPath())
                .status(requestLog.getStatus())
                .latencyMs(requestLog.getLatencyMs())
                .clientIp(requestLog.getClientIp())
                .rateLimited(requestLog.getRateLimited())
                .timestamp(requestLog.getTimestamp())
                .apiKeyName(apiKeyName)
                .serviceName(serviceName)
                .build();
    }

    /**
     * Get top endpoints by request count
     * 
     * Features:
     * - Restricted to last 24 hours only
     * - Aggregates by path + serviceRouteId
     * - Calculates requestCount, avgLatencyMs, errorCount (status >= 400)
     * - No response structure changes (backward compatible)
     */
    public List<TopEndpoint> getTopEndpoints(int limit) {
        Instant now = Instant.now();
        Instant yesterday = now.minus(24, ChronoUnit.HOURS);

        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> results = requestLogRepository.findTopEndpointsBetween(yesterday, now, pageable);

        List<TopEndpoint> topEndpoints = new ArrayList<>();
        for (Object[] row : results) {
            TopEndpoint endpoint = TopEndpoint.builder()
                    .path((String) row[0])
                    .serviceRouteId(((Number) row[1]).longValue())
                    .requestCount(((Number) row[2]).longValue())
                    .avgLatencyMs(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                    .errorCount(((Number) row[4]).longValue())
                    .build();
            topEndpoints.add(endpoint);
        }

        return topEndpoints;
    }
}
