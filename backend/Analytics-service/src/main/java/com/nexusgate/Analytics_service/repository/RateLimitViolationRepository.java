package com.nexusgate.Analytics_service.repository;

import com.nexusgate.Analytics_service.model.RateLimitViolation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * RateLimitViolationRepository
 * 
 * Provides data access for rate limit violation tracking
 * Used by dashboard and analytics services
 */
@Repository
public interface RateLimitViolationRepository extends JpaRepository<RateLimitViolation, UUID> {

    /**
     * Count violations by date
     * Used for daily violation trends on dashboard
     * 
     * @param start Start of the day
     * @param end End of the day
     * @return Count of violations
     */
    @Query("SELECT COUNT(v) FROM RateLimitViolation v WHERE v.timestamp BETWEEN :start AND :end")
    Long countViolationsByDate(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Find recent violations with pagination
     * Returns violations sorted by timestamp descending (newest first)
     * 
     * @param pageable Pagination parameters
     * @return Page of violations
     */
    Page<RateLimitViolation> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Find recent violations within time range
     * 
     * @param start Start time
     * @param end End time
     * @param pageable Pagination parameters
     * @return Page of violations
     */
    Page<RateLimitViolation> findByTimestampBetweenOrderByTimestampDesc(
            Instant start, Instant end, Pageable pageable);

    /**
     * Count violations for specific API key in time range
     * Useful for per-client violation tracking
     * 
     * @param apiKey API key to check
     * @param start Start time
     * @param end End time
     * @return Count of violations
     */
    @Query("SELECT COUNT(v) FROM RateLimitViolation v WHERE v.apiKey = :apiKey AND v.timestamp BETWEEN :start AND :end")
    Long countViolationsByApiKey(
            @Param("apiKey") String apiKey,
            @Param("start") Instant start,
            @Param("end") Instant end);

    /**
     * Count violations for specific service in time range
     * Useful for per-service violation tracking
     * 
     * @param serviceName Service name
     * @param start Start time
     * @param end End time
     * @return Count of violations
     */
    @Query("SELECT COUNT(v) FROM RateLimitViolation v WHERE v.serviceName = :serviceName AND v.timestamp BETWEEN :start AND :end")
    Long countViolationsByService(
            @Param("serviceName") String serviceName,
            @Param("start") Instant start,
            @Param("end") Instant end);

    /**
     * Find violations by API key
     * 
     * @param apiKey API key
     * @param pageable Pagination
     * @return Page of violations
     */
    Page<RateLimitViolation> findByApiKeyOrderByTimestampDesc(String apiKey, Pageable pageable);

    /**
     * Find violations by service name
     * 
     * @param serviceName Service name
     * @param pageable Pagination
     * @return Page of violations
     */
    Page<RateLimitViolation> findByServiceNameOrderByTimestampDesc(String serviceName, Pageable pageable);

    /**
     * Get top violating API keys in time range
     * Returns API keys with the most violations
     * 
     * @param start Start time
     * @param end End time
     * @param pageable Limit results
     * @return List of API keys with violation count
     */
    @Query("SELECT v.apiKey, COUNT(v) as violationCount " +
           "FROM RateLimitViolation v " +
           "WHERE v.timestamp BETWEEN :start AND :end " +
           "GROUP BY v.apiKey " +
           "ORDER BY violationCount DESC")
    List<Object[]> findTopViolatingApiKeys(
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);
}
