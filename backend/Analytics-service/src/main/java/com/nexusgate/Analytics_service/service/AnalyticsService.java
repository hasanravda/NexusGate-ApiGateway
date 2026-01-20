package com.nexusgate.Analytics_service.service;

import com.nexusgate.Analytics_service.dto.AnalyticsOverview;
import com.nexusgate.Analytics_service.dto.TopEndpoint;
import com.nexusgate.Analytics_service.model.RequestLog;
import com.nexusgate.Analytics_service.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * AnalyticsService
 * 
 * Provides read-only analytics queries from PostgreSQL
 * Used by AnalyticsController to serve dashboard data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final RequestLogRepository requestLogRepository;

    /**
     * Get analytics overview for the last 24 hours
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
     */
    public Page<RequestLog> getRecentRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return requestLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    /**
     * Get top endpoints by request count
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
