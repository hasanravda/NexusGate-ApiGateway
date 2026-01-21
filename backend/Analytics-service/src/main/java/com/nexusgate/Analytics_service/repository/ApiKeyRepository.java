package com.nexusgate.Analytics_service.repository;

import com.nexusgate.Analytics_service.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ApiKey entity (read-only lookups)
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /**
     * Find API key by ID for name enrichment
     */
    Optional<ApiKey> findById(Long id);
}
