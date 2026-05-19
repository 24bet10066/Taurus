package com.serviceos.job.service;

import com.serviceos.job.exception.RateLimitExceededException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class PublicBookingRateLimiter {

    private static final String KEY_PREFIX = "booking:rate:";
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int LIMIT = 3;

    private final StringRedisTemplate redis;

    public PublicBookingRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void checkAndIncrement(String phone) {
        String key = KEY_PREFIX + phone;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, WINDOW);
        }
        if (count != null && count > LIMIT) {
            Long ttl = redis.getExpire(key);
            long retryAfter = ttl != null && ttl > 0 ? ttl : WINDOW.toSeconds();
            throw new RateLimitExceededException(
                    "Booking limit exceeded. Max " + LIMIT + " bookings per hour. Retry after " + retryAfter + "s."
            );
        }
    }
}
