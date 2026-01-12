package com.nexusgate.config_service.repository;

import com.nexusgate.config_service.model.ServiceRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRouteRepository extends JpaRepository<ServiceRoute, Long> {
    
    // Find by public path
    Optional<ServiceRoute> findByPublicPath(String publicPath);
    
    // Find all active routes
    List<ServiceRoute> findByIsActiveTrue();
    
    // Find by service name
    List<ServiceRoute> findByServiceNameContainingIgnoreCase(String serviceName);
    
    // Check if public path exists
    boolean existsByPublicPath(String publicPath);
}