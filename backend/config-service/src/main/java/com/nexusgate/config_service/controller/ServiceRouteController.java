package com.nexusgate.config_service.controller;

import com.nexusgate.config_service.dto.CreateServiceRouteRequest;
import com.nexusgate.config_service.dto.ServiceRouteDto;
import com.nexusgate.config_service.service.ServiceRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/service-routes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ServiceRouteController {

    private final ServiceRouteService serviceRouteService;

    /**
     * Create new service route
     * POST /service-routes
     */
    @PostMapping
    public ResponseEntity<ServiceRouteDto> createServiceRoute(@RequestBody CreateServiceRouteRequest request) {
        ServiceRouteDto created = serviceRouteService.createServiceRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get all service routes
     * GET /service-routes
     */
    @GetMapping
    public ResponseEntity<List<ServiceRouteDto>> getAllServiceRoutes(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {

        List<ServiceRouteDto> routes = activeOnly
                ? serviceRouteService.getActiveServiceRoutes()
                : serviceRouteService.getAllServiceRoutes();

        return ResponseEntity.ok(routes);
    }

    /**
     * Get service route by ID
     * GET /service-routes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceRouteDto> getServiceRouteById(@PathVariable Long id) {
        ServiceRouteDto route = serviceRouteService.getServiceRouteById(id);
        return ResponseEntity.ok(route);
    }

    /**
     * Get service route by public path
     * GET /service-routes/by-path?path=/api/users/**
     */
    @GetMapping("/by-path")
    public ResponseEntity<ServiceRouteDto> getServiceRouteByPath(@RequestParam String path) {
        ServiceRouteDto route = serviceRouteService.getServiceRouteByPath(path);
        return ResponseEntity.ok(route);
    }

    /**
     * Update service route
     * PUT /service-routes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ServiceRouteDto> updateServiceRoute(
            @PathVariable Long id,
            @RequestBody CreateServiceRouteRequest request) {

        ServiceRouteDto updated = serviceRouteService.updateServiceRoute(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Toggle active status
     * PATCH /service-routes/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ServiceRouteDto> toggleActiveStatus(@PathVariable Long id) {
        ServiceRouteDto updated = serviceRouteService.toggleActiveStatus(id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete service route
     * DELETE /service-routes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceRoute(@PathVariable Long id) {
        serviceRouteService.deleteServiceRoute(id);
        return ResponseEntity.noContent().build();
    }
}