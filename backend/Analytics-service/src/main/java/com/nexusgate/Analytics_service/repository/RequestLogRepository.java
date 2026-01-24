package com.nexusgate.Analytics_service.repository;

import com.nexusgate.Analytics_service.model.RequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * RequestLogRepository
 * 
 * Data access for request logs
 * Provides queries for analytics and dashboard
 */
@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    /**
     * Find recent logs with pagination
     */
    Page<RequestLog> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Find logs within a time range
     */
    List<RequestLog> findByTimestampBetween(Instant start, Instant end);

    /**
     * Count total requests in time range
     */
    @Query("SELECT COUNT(r) FROM RequestLog r WHERE r.timestamp BETWEEN :start AND :end")
    Long countRequestsBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Count errors (status >= 400) in time range
     */
    @Query("SELECT COUNT(r) FROM RequestLog r WHERE r.status >= 400 AND r.timestamp BETWEEN :start AND :end")
    Long countErrorsBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Count rate limit violations in time range
     */
    @Query("SELECT COUNT(r) FROM RequestLog r WHERE r.rateLimited = true AND r.timestamp BETWEEN :start AND :end")
    Long countRateLimitViolationsBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Calculate average latency in time range
     */
    @Query("SELECT AVG(r.latencyMs) FROM RequestLog r WHERE r.timestamp BETWEEN :start AND :end")
    Double averageLatencyBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Find top endpoints by request count
     */
    @Query("SELECT r.path as path, r.serviceRouteId as serviceRouteId, COUNT(r) as requestCount, " +
           "AVG(r.latencyMs) as avgLatencyMs, SUM(CASE WHEN r.status >= 400 THEN 1 ELSE 0 END) as errorCount " +
           "FROM RequestLog r " +
           "WHERE r.timestamp BETWEEN :start AND :end " +
           "GROUP BY r.path, r.serviceRouteId " +
           "ORDER BY requestCount DESC")
    List<Object[]> findTopEndpointsBetween(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);

    /**
     * Count blocked requests (authentication/authorization failures)
     * Dashboard requirement: blocked requests count
     */
    @Query("SELECT COUNT(r) FROM RequestLog r WHERE r.blocked = true")
    Long countBlockedRequests();

    /**
     * Count blocked requests in time range
     */
    @Query("SELECT COUNT(r) FROM RequestLog r WHERE r.blocked = true AND r.timestamp BETWEEN :start AND :end")
    Long countBlockedRequestsBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Calculate average latency for last 24 hours
     * Dashboard requirement: average response time
     */
    @Query("SELECT COALESCE(AVG(r.latencyMs), 0.0) FROM RequestLog r WHERE r.timestamp >= :since")
    Double averageLatencyLast24Hours(@Param("since") Instant since);

    /**
     * Count requests by date (for specific day)
     * Dashboard requirement: daily request counts
     */
    @Query("SELECT COUNT(r) FROM RequestLog r WHERE r.timestamp BETWEEN :start AND :end")
    Long countRequestsByDate(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Find blocked requests with pagination
     */
    Page<RequestLog> findByBlockedTrueOrderByTimestampDesc(Pageable pageable);

    /**
     * Find rate-limited requests with pagination
     */
    Page<RequestLog> findByRateLimitedTrueOrderByTimestampDesc(Pageable pageable);
}

