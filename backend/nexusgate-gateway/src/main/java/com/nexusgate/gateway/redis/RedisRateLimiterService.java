package com.nexusgate.gateway.redis;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimiterService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public Mono<Boolean> isAllowed(Long apiKeyId, Long serviceRouteId, Integer requestsPerMinute, Integer requestsPerHour) {
        String minuteKey = String.format("nexusgate:%d:%d:minute", apiKeyId, serviceRouteId);
        String hourKey = String.format("nexusgate:%d:%d:hour", apiKeyId, serviceRouteId);

        return checkAndIncrement(minuteKey, requestsPerMinute, Duration.ofMinutes(1))
                .flatMap(minuteAllowed -> {
                    if (!minuteAllowed) {
                        return Mono.just(false);
                    }
                    return checkAndIncrement(hourKey, requestsPerHour, Duration.ofHours(1));
                });
    }

    private Mono<Boolean> checkAndIncrement(String key, Integer limit, Duration expiration) {
        if (limit == null || limit <= 0) {
            return Mono.just(true);
        }

        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(key, expiration)
                                .thenReturn(true);
                    }
                    return Mono.just(count <= limit);
                })
                .onErrorResume(e -> {
                    log.error("Redis error for key: {}", key, e);
                    return Mono.just(true);
                });
    }
}