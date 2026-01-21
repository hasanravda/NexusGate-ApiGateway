package com.nexusgate.Analytics_service.service;

import com.nexusgate.Analytics_service.model.MetricsSummary;
import com.nexusgate.Analytics_service.model.RequestLog;
import com.nexusgate.Analytics_service.repository.MetricsSummaryRepository;
import com.nexusgate.Analytics_service.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AggregationService
 * 
 * Runs scheduled jobs to aggregate raw RequestLog data into MetricsSummary
 * Used for dashboards, reports, and billing
 * 
 * Runs daily at 2 AM to aggregate previous day's data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {

    private final RequestLogRepository requestLogRepository;
    private final MetricsSummaryRepository metricsSummaryRepository;

    /**
     * Scheduled job: Aggregate yesterday's data
     * Runs daily at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void aggregateDailyMetrics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting daily aggregation for date: {}", yesterday);

        try {
            aggregateForDate(yesterday);
            log.info("Completed daily aggregation for date: {}", yesterday);
        } catch (Exception e) {
            log.error("Failed to aggregate metrics for date: {}", yesterday, e);
        }
    }

    /**
     * Aggregate metrics for a specific date
     */
    public void aggregateForDate(LocalDate date) {
        // Define time range for the date
        Instant startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        // Fetch all logs for the date
        List<RequestLog> logs = requestLogRepository.findByTimestampBetween(startOfDay, endOfDay);

        if (logs.isEmpty()) {
            log.info("No logs found for date: {}", date);
            return;
        }

        // Group by serviceRouteId
        Map<Long, List<RequestLog>> logsByRoute = logs.stream()
                .collect(Collectors.groupingBy(RequestLog::getServiceRouteId));

        // Aggregate for each route
        for (Map.Entry<Long, List<RequestLog>> entry : logsByRoute.entrySet()) {
            Long serviceRouteId = entry.getKey();
            List<RequestLog> routeLogs = entry.getValue();

            aggregateForServiceRoute(date, serviceRouteId, routeLogs);
        }
    }

    /**
     * Aggregate metrics for a specific service route
     */
    private void aggregateForServiceRoute(LocalDate date, Long serviceRouteId, List<RequestLog> logs) {
        long totalRequests = logs.size();
        long errorCount = logs.stream().filter(log -> log.getStatus() >= 400).count();
        
        double avgLatency = logs.stream()
                .mapToLong(RequestLog::getLatencyMs)
                .average()
                .orElse(0.0);

        // Calculate P95 latency (approximate)
        long p95Latency = calculateP95Latency(logs);

        // Check if summary already exists
        MetricsSummary existing = metricsSummaryRepository
                .findByDateAndServiceRouteId(date, serviceRouteId)
                .orElse(null);

        if (existing != null) {
            // Update existing summary
            existing.setTotalRequests(totalRequests);
            existing.setErrorCount(errorCount);
            existing.setAvgLatencyMs(avgLatency);
            existing.setP95LatencyMs(p95Latency);
            metricsSummaryRepository.save(existing);
        } else {
            // Create new summary
            MetricsSummary summary = MetricsSummary.builder()
                    .date(date)
                    .serviceRouteId(serviceRouteId)
                    .totalRequests(totalRequests)
                    .errorCount(errorCount)
                    .avgLatencyMs(avgLatency)
                    .p95LatencyMs(p95Latency)
                    .build();
            metricsSummaryRepository.save(summary);
        }

        log.debug("Aggregated metrics for date={}, serviceRouteId={}, totalRequests={}",
                date, serviceRouteId, totalRequests);
    }

    /**
     * Calculate P95 latency (approximate)
     */
    private long calculateP95Latency(List<RequestLog> logs) {
        List<Long> latencies = logs.stream()
                .map(RequestLog::getLatencyMs)
                .sorted()
                .toList();

        if (latencies.isEmpty()) {
            return 0L;
        }

        int p95Index = (int) Math.ceil(latencies.size() * 0.95) - 1;
        p95Index = Math.max(0, Math.min(p95Index, latencies.size() - 1));

        return latencies.get(p95Index);
    }
}
