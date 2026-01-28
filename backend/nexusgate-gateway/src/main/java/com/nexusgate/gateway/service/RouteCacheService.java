package com.nexusgate.gateway.service;

import com.nexusgate.gateway.client.ServiceRouteClient;
import com.nexusgate.gateway.dto.ServiceRouteResponse;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service to cache routes and refresh them periodically.
 * Prevents repeated calls to config-service on every request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCacheService {

    private final ServiceRouteClient serviceRouteClient;
    
    // Thread-safe list for cached routes
    private final List<ServiceRouteResponse> cachedRoutes = new CopyOnWriteArrayList<>();
    
    private volatile boolean cacheInitialized = false;
    private volatile long lastSuccessfulRefresh = 0;
    private volatile boolean configServiceAvailable = true;

    /**
     * Initialize cache on startup
     */
    @PostConstruct
    public void init() {
        log.info("Initializing route cache...");
        refreshRoutes();
    }

    /**
     * Refresh routes every 30 seconds (reduced noise)
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 5000)
    public void refreshRoutes() {
        
        serviceRouteClient.getAllActiveRoutes()
                .collectList()
                .subscribe(
                        routes -> {
                            boolean wasEmpty = cachedRoutes.isEmpty();
                            boolean wasUnavailable = !configServiceAvailable;
                            
                            cachedRoutes.clear();
                            cachedRoutes.addAll(routes);
                            cacheInitialized = true;
                            lastSuccessfulRefresh = System.currentTimeMillis();
                            configServiceAvailable = !routes.isEmpty();
                            
                            if (routes.isEmpty() && !wasEmpty) {
                                log.warn("⚠️ Route cache cleared - no active routes available");
                            } else if (routes.isEmpty() && !wasUnavailable) {
                                log.warn("⚠️ Config service unavailable - Gateway will return 404 for all requests");
                            } else if (!routes.isEmpty() && wasUnavailable) {
                                log.info("✓ Config service recovered! Loaded {} active routes", routes.size());
                                routes.forEach(route -> 
                                    log.debug("  → Route: {} → {}", route.getPublicPath(), route.getTargetUrl())
                                );
                            } else if (!routes.isEmpty()) {
                                log.info("✓ Route cache refreshed: {} active routes", routes.size());
                            }
                        },
                        error -> {
                            if (!cacheInitialized) {
                                log.warn("⚠️ Initial cache load failed - Config service unavailable");
                                cachedRoutes.clear();
                                cacheInitialized = true;
                                configServiceAvailable = false;
                            } else if (configServiceAvailable) {
                                log.warn("⚠️ Config service became unavailable - Keeping {} existing routes", cachedRoutes.size());
                                configServiceAvailable = false;
                            }
                            // Silent after first failure - no need to spam logs every 30 seconds
                        }
                );
    }

    /**
     * Get cached routes as Flux.
     * Returns immediately without calling config-service.
     */
    public Flux<ServiceRouteResponse> getCachedRoutes() {
        if (!cacheInitialized) {
            log.warn("Route cache not initialized yet. Returning empty list.");
        }
        
        // Return cached routes without making external calls
        return Flux.fromIterable(new ArrayList<>(cachedRoutes));
    }

    /**
     * Get cache statistics for monitoring
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
                cacheInitialized,
                cachedRoutes.size(),
                lastSuccessfulRefresh,
                System.currentTimeMillis() - lastSuccessfulRefresh
        );
    }

    @Getter
    public static class CacheStats {
        private final boolean initialized;
        private final int routeCount;
        private final long lastRefreshTime;
        private final long timeSinceLastRefresh;

        public CacheStats(boolean initialized, int routeCount, long lastRefreshTime, long timeSinceLastRefresh) {
            this.initialized = initialized;
            this.routeCount = routeCount;
            this.lastRefreshTime = lastRefreshTime;
            this.timeSinceLastRefresh = timeSinceLastRefresh;
        }

    }
}
