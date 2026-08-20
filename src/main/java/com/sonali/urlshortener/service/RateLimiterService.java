package com.sonali.urlshortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Fixed-window rate limiter backed by Redis.
 *
 * Each client (identified by IP) gets a counter key that increments on every
 * request. The first increment in a window sets a TTL equal to the window
 * length, so the counter resets automatically once the window expires.
 * Because the counter lives in Redis rather than in-process memory, this
 * limit is enforced correctly even if the app is scaled to multiple
 * instances behind a load balancer.
 */
@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final int maxRequests;
    private final Duration windowDuration;

    public RateLimiterService(StringRedisTemplate redisTemplate,
                               @Value("${rate-limit.max-requests:10}") int maxRequests,
                               @Value("${rate-limit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowDuration = Duration.ofSeconds(windowSeconds);
    }

    /**
     * Increments the request count for the given key (e.g. client IP) and
     * returns true if the caller is still within the allowed limit.
     */
    public boolean isAllowed(String key) {
        String redisKey = "rate_limit:" + key;

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == null) {
            // Redis unreachable / unexpected response — fail open rather than
            // blocking traffic because of a caching-layer outage.
            return true;
        }

        if (count == 1L) {
            redisTemplate.expire(redisKey, windowDuration);
        }

        return count <= maxRequests;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public long getWindowSeconds() {
        return windowDuration.getSeconds();
    }
}