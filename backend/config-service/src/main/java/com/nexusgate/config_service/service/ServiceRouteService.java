package com.nexusgate.config_service.service;

import com.nexusgate.config_service.dto.CreateServiceRouteRequest;
import com.nexusgate.config_service.dto.ServiceRouteDto;
import com.nexusgate.config_service.dto.UpdateSecurityRequest;
import com.nexusgate.config_service.exception.ResourceNotFoundException;
import com.nexusgate.config_service.model.ServiceRoute;
import com.nexusgate.config_service.repository.ServiceRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRouteService {

    private final ServiceRouteRepository serviceRouteRepository;

    // Create new service route
    @Transactional
    public ServiceRouteDto createServiceRoute(CreateServiceRouteRequest request) {

        // Check if public path already exists
        if (serviceRouteRepository.existsByPublicPath(request.getPublicPath())) {
            throw new IllegalArgumentException(
                "Public path already exists: " + request.getPublicPath()
            );
        }

        ServiceRoute serviceRoute = ServiceRoute.builder()
                .serviceName(request.getServiceName())
                .serviceDescription(request.getServiceDescription())
                .publicPath(request.getPublicPath())
                .targetUrl(request.getTargetUrl())
                .allowedMethods(
                    request.getAllowedMethods() != null && !request.getAllowedMethods().isEmpty()
                        ? "{" + String.join(",", request.getAllowedMethods()) + "}"
                        : "{GET,POST,PUT,DELETE}"
                )
                .requiresApiKey(
                    request.getRequiresApiKey() != null
                        ? request.getRequiresApiKey()
                        : true  // Default to true for backward compatibility
                )
                .rateLimitPerMinute(
                    request.getRateLimitPerMinute() != null
                        ? request.getRateLimitPerMinute()
                        : 100
                )
                .rateLimitPerHour(
                    request.getRateLimitPerHour() != null
                        ? request.getRateLimitPerHour()
                        : 5000
                )
                .isActive(true)
                .createdByUserId(request.getCreatedByUserId())
                .notes(request.getNotes())
                .build();

        ServiceRoute saved = serviceRouteRepository.save(serviceRoute);
        return toDto(saved);
    }

    // Get all service routes
    @Transactional(readOnly = true)
    public List<ServiceRouteDto> getAllServiceRoutes() {
        return serviceRouteRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Get active service routes only
    @Transactional(readOnly = true)
    public List<ServiceRouteDto> getActiveServiceRoutes() {
        return serviceRouteRepository.findByIsActiveTrue()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Get service route by ID
    @Transactional(readOnly = true)
    public ServiceRouteDto getServiceRouteById(Long id) {
        ServiceRoute serviceRoute = serviceRouteRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Service route not found with id: " + id)
                );
        return toDto(serviceRoute);
    }

    // Get service route by public path
    @Transactional(readOnly = true)
    public ServiceRouteDto getServiceRouteByPath(String publicPath) {
        ServiceRoute serviceRoute = serviceRouteRepository.findByPublicPath(publicPath)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Service route not found with path: " + publicPath)
                );
        return toDto(serviceRoute);
    }

    // Update service route
    @Transactional
    public ServiceRouteDto updateServiceRoute(Long id, CreateServiceRouteRequest request) {

        ServiceRoute existing = serviceRouteRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Service route not found with id: " + id)
                );

        // Check if new public path conflicts with another route
        if (
            request.getPublicPath() != null &&
            !existing.getPublicPath().equals(request.getPublicPath()) &&
            serviceRouteRepository.existsByPublicPath(request.getPublicPath())
        ) {
            throw new IllegalArgumentException(
                "Public path already exists: " + request.getPublicPath()
            );
        }

        existing.setServiceName(request.getServiceName());
        existing.setServiceDescription(request.getServiceDescription());
        existing.setPublicPath(request.getPublicPath());
        existing.setTargetUrl(request.getTargetUrl());
        existing.setAllowedMethods(
            request.getAllowedMethods() != null && !request.getAllowedMethods().isEmpty()
                ? "{" + String.join(",", request.getAllowedMethods()) + "}"
                : existing.getAllowedMethods()
        );
        if (request.getRequiresApiKey() != null) {
            existing.setRequiresApiKey(request.getRequiresApiKey());
        }
        existing.setRateLimitPerMinute(request.getRateLimitPerMinute());
        existing.setRateLimitPerHour(request.getRateLimitPerHour());
        existing.setNotes(request.getNotes());

        ServiceRoute updated = serviceRouteRepository.save(existing);
        return toDto(updated);
    }

    // Toggle active status
    @Transactional
    public ServiceRouteDto toggleActiveStatus(Long id) {
        ServiceRoute serviceRoute = serviceRouteRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Service route not found with id: " + id)
                );

        serviceRoute.setIsActive(!serviceRoute.getIsActive());
        ServiceRoute updated = serviceRouteRepository.save(serviceRoute);
        return toDto(updated);
    }

    // Update security settings (requiresApiKey)
    @Transactional
    public ServiceRouteDto updateSecurity(Long id, UpdateSecurityRequest request) {
        ServiceRoute serviceRoute = serviceRouteRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Service route not found with id: " + id)
                );

        if (request.getRequiresApiKey() == null) {
            throw new IllegalArgumentException("requiresApiKey field is required");
        }

        serviceRoute.setRequiresApiKey(request.getRequiresApiKey());
        ServiceRoute updated = serviceRouteRepository.save(serviceRoute);
        return toDto(updated);
    }

    // Delete service route
    @Transactional
    public void deleteServiceRoute(Long id) {
        if (!serviceRouteRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                "Service route not found with id: " + id
            );
        }
        serviceRouteRepository.deleteById(id);
    }

    // Helper: Convert entity to DTO
    private ServiceRouteDto toDto(ServiceRoute serviceRoute) {
        return ServiceRouteDto.builder()
                .id(serviceRoute.getId())
                .serviceName(serviceRoute.getServiceName())
                .serviceDescription(serviceRoute.getServiceDescription())
                .publicPath(serviceRoute.getPublicPath())
                .targetUrl(serviceRoute.getTargetUrl())
                .allowedMethods(
                    serviceRoute.getAllowedMethods() != null
                        ? Arrays.asList(serviceRoute.getAllowedMethods().replaceAll("[{}]", "").split(","))
                        : java.util.Collections.emptyList()
                )
                .requiresApiKey(serviceRoute.getRequiresApiKey())
                .rateLimitPerMinute(serviceRoute.getRateLimitPerMinute())
                .rateLimitPerHour(serviceRoute.getRateLimitPerHour())
                .isActive(serviceRoute.getIsActive())
                .createdByUserId(serviceRoute.getCreatedByUserId())
                .createdAt(serviceRoute.getCreatedAt())
                .updatedAt(serviceRoute.getUpdatedAt())
                .notes(serviceRoute.getNotes())
                .build();
    }
}