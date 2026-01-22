package com.nexusgate.gateway.service;

import com.nexusgate.gateway.client.ApiKeyClient;
import com.nexusgate.gateway.dto.ApiKeyResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Caches API keys to avoid calling config-service on every request.
 * Refreshes cache every 60 seconds to pick up new/updated API keys.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyCacheService {

    private final ApiKeyClient apiKeyClient;

    // Cache: key = API key string, value = ApiKeyResponse
    private final ConcurrentHashMap<String, ApiKeyResponse> apiKeyCache = new ConcurrentHashMap<>();
    
    // List of all cached API keys for admin purposes
    private final List<ApiKeyResponse> allApiKeys = new CopyOnWriteArrayList<>();

    /**
     * Initialize cache on startup
     */
    @PostConstruct
    public void init() {
        log.info("Initializing API key cache...");
        refreshCache();
    }

    /**
     * Refresh cache every 60 seconds
     */
    @Scheduled(fixedDelay = 60000)
    public void refreshCache() {
        log.debug("Refreshing API key cache...");
        
        apiKeyClient.getAllApiKeys()
                .collectList()
                .subscribe(
                        apiKeys -> {
                            // Clear old cache
                            apiKeyCache.clear();
                            allApiKeys.clear();
                            
                            // Rebuild cache
                            apiKeys.forEach(apiKey -> {
                                apiKeyCache.put(apiKey.getKeyValue(), apiKey);
                                allApiKeys.add(apiKey);
                            });
                            
                            log.info("API key cache refreshed successfully. Loaded {} active API keys", apiKeys.size());
                        },
                        error -> {
                            log.error("Failed to refresh API key cache. Using existing cache. Error: {}", 
                                    error.getMessage());
                        }
                );
    }

    /**
     * Validate API key from cache (instant lookup, zero network calls)
     * 
     * @param apiKey The API key to validate
     * @return Mono<ApiKeyResponse> if valid, Mono.error() if invalid
     */
    public Mono<ApiKeyResponse> validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("API key is required"));
        }

        // Instant cache lookup - no network call!
        ApiKeyResponse cached = apiKeyCache.get(apiKey);
        
        if (cached == null) {
            log.warn("API key not found in cache: {}", maskApiKey(apiKey));
            return Mono.error(new IllegalArgumentException("Invalid API key"));
        }
        
        log.debug("API key found in cache: {} -> ApiKeyId: {}", maskApiKey(apiKey), cached.getId());
        return Mono.just(cached);
    }

    /**
     * Get all cached API keys (for admin/monitoring purposes)
     */
    public List<ApiKeyResponse> getAllCachedApiKeys() {
        return List.copyOf(allApiKeys);
    }

    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
                apiKeyCache.size(),
                allApiKeys.size()
        );
    }

    /**
     * Mask API key for logging (show first 8 chars, hide rest)
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 8) + "***";
    }

    /**
     * Cache statistics for monitoring
     */
    public record CacheStats(int cachedKeys, int totalKeys) {}
}
