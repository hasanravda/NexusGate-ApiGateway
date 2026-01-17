package com.nexusgate.config_service.repository;

import com.nexusgate.config_service.model.RateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateLimitRepository extends JpaRepository<RateLimit, Long> {

    // Find all rate limits for a specific API key
    List<RateLimit> findByApiKeyId(Long apiKeyId);

    // Find all rate limits for a specific service route
    List<RateLimit> findByServiceRouteId(Long serviceRouteId);

    // Find specific rate limit for API key + service route combination
    Optional<RateLimit> findByApiKeyIdAndServiceRouteId(Long apiKeyId, Long serviceRouteId);

    // Find default rate limit for service route (api_key_id = null)
    Optional<RateLimit> findByApiKeyIdIsNullAndServiceRouteId(Long serviceRouteId);

    // Find global rate limit for API key (service_route_id = null)
    Optional<RateLimit> findByApiKeyIdAndServiceRouteIdIsNull(Long apiKeyId);

    // Check if rate limit exists for combination
    boolean existsByApiKeyIdAndServiceRouteId(Long apiKeyId, Long serviceRouteId);
}
