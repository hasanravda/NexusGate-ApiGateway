package com.nexusgate.config_service.repository;

import com.nexusgate.config_service.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey,Long> {
    Optional<ApiKey> findByKeyValue(String keyValue);
    List<ApiKey> findByCreatedByUserId(Long userId);
    List<ApiKey> findByCreatedByUserIdAndIsActive(Long userId, Boolean isActive);
    List<ApiKey> findByClientName(String clientName);
}
