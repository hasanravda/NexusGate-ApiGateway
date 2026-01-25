package com.nexusgate.gateway.redis;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Token Bucket Rate Limiting Algorithm Implementation
 * 
 * Algorithm Overview:
 * - Each API key/route has a bucket with a maximum capacity of tokens
 * - Tokens are refilled at a constant rate (tokens per second)
 * - Each request consumes 1 token
 * - If bucket has tokens available, request is allowed (token consumed)
 * - If bucket is empty, request is denied
 * 
 * Benefits over Fixed Window:
 * - No burst at window boundaries
 * - Smooth traffic distribution
 * - Allows small bursts up to capacity, then enforces steady rate
 * 
 * Redis Storage:
 * - Hash: tokens (current count), last_refill (timestamp in seconds)
 * - TTL: Auto-expire after 1 hour of inactivity
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimiterService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * Check if request is allowed using Token Bucket algorithm
     * Checks both per-minute and per-hour buckets
     */
    public Mono<Boolean> isAllowed(Long apiKeyId, Long serviceRouteId, Integer requestsPerMinute, Integer requestsPerHour) {
        String minuteKey = String.format("rate:bucket:%d:%d:minute", apiKeyId, serviceRouteId);
        String hourKey = String.format("rate:bucket:%d:%d:hour", apiKeyId, serviceRouteId);

        log.debug("Checking rate limits (Token Bucket) - ApiKeyId: {}, RouteId: {}, PerMinute: {}, PerHour: {}", 
                apiKeyId, serviceRouteId, requestsPerMinute, requestsPerHour);

        // Check minute bucket first
        return tryConsumeToken(minuteKey, requestsPerMinute, 60)
                .flatMap(minuteAllowed -> {
                    if (!minuteAllowed) {
                        log.warn("Rate limit exceeded for minute bucket - ApiKeyId: {}, RouteId: {}", apiKeyId, serviceRouteId);
                        return Mono.just(false);
                    }
                    // If minute bucket allows, check hour bucket
                    return tryConsumeToken(hourKey, requestsPerHour, 3600)
                            .doOnNext(hourAllowed -> {
                                if (!hourAllowed) {
                                    log.warn("Rate limit exceeded for hour bucket - ApiKeyId: {}, RouteId: {}", apiKeyId, serviceRouteId);
                                }
                            });
                });
    }

    /**
     * Token Bucket Algorithm Implementation
     * 
     * @param bucketKey Redis key for the bucket
     * @param capacity Maximum tokens in bucket (burst capacity)
     * @param refillIntervalSeconds Time window for refill (60 for minute, 3600 for hour)
     * @return true if token consumed successfully, false if bucket empty
     */
    private Mono<Boolean> tryConsumeToken(String bucketKey, Integer capacity, int refillIntervalSeconds) {
        if (capacity == null || capacity <= 0) {
            return Mono.just(true); // No limit configured
        }

        long currentTimeSeconds = Instant.now().getEpochSecond();
        double refillRate = (double) capacity / refillIntervalSeconds; // tokens per second

        return redisTemplate.opsForHash().get(bucketKey, "tokens")
                .defaultIfEmpty("0")
                .zipWith(
                    redisTemplate.opsForHash().get(bucketKey, "last_refill")
                        .defaultIfEmpty(String.valueOf(currentTimeSeconds))
                )
                .flatMap(tuple -> {
                    double currentTokens = Double.parseDouble(tuple.getT1());
                    long lastRefillTime = Long.parseLong(tuple.getT2());
                    
                    // Calculate tokens to add based on elapsed time
                    long elapsedSeconds = currentTimeSeconds - lastRefillTime;
                    double tokensToAdd = elapsedSeconds * refillRate;
                    
                    // Refill tokens (capped at capacity)
                    double newTokens = Math.min(capacity, currentTokens + tokensToAdd);
                    
                    log.debug("Token Bucket - Key: {}, Current: {}, Elapsed: {}s, ToAdd: {}, New: {}, Capacity: {}", 
                            bucketKey, currentTokens, elapsedSeconds, tokensToAdd, newTokens, capacity);
                    
                    // Try to consume 1 token
                    if (newTokens >= 1.0) {
                        // Token available - consume it
                        double remainingTokens = newTokens - 1.0;
                        
                        return redisTemplate.opsForHash().put(bucketKey, "tokens", String.valueOf(remainingTokens))
                                .then(redisTemplate.opsForHash().put(bucketKey, "last_refill", String.valueOf(currentTimeSeconds)))
                                .then(redisTemplate.expire(bucketKey, java.time.Duration.ofHours(1))) // Auto-cleanup
                                .thenReturn(true)
                                .doOnSuccess(allowed -> 
                                    log.debug("Token consumed - Key: {}, Remaining: {}", bucketKey, remainingTokens)
                                );
                    } else {
                        // No tokens available - deny request
                        log.debug("No tokens available - Key: {}, Tokens: {}", bucketKey, newTokens);
                        
                        // Update last refill time even on denial (prevent drift)
                        return redisTemplate.opsForHash().put(bucketKey, "tokens", String.valueOf(newTokens))
                                .then(redisTemplate.opsForHash().put(bucketKey, "last_refill", String.valueOf(currentTimeSeconds)))
                                .then(redisTemplate.expire(bucketKey, java.time.Duration.ofHours(1)))
                                .thenReturn(false);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Redis error for token bucket key: {}", bucketKey, e);
                    return Mono.just(true); // Fail open - allow request on error
                });
    }
}