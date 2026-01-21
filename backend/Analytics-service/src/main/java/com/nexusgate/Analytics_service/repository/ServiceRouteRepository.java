package com.nexusgate.Analytics_service.repository;

import com.nexusgate.Analytics_service.model.ServiceRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ServiceRoute entity (read-only lookups)
 */
@Repository
public interface ServiceRouteRepository extends JpaRepository<ServiceRoute, Long> {

    /**
     * Find service route by ID for name enrichment
     */
    Optional<ServiceRoute> findById(Long id);
}
