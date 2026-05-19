package com.serviceos.auth.service;

import com.serviceos.auth.config.AuthProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private static final String OTP_SEND_KEY = "otp:rate:send:%s";
    private static final Duration OTP_SEND_WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redis;
    private final AuthProperties props;

    public RateLimitService(StringRedisTemplate redis, AuthProperties props) {
        this.redis = redis;
        this.props = props;
    }

    public Result tryRegisterOtpSend(String phone) {
        String key = OTP_SEND_KEY.formatted(phone);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, OTP_SEND_WINDOW);
        }
        long current = count == null ? 0L : count;
        int limit = props.otp().sendRateLimitPerHour();
        if (current > limit) {
            Long ttl = redis.getExpire(key);
            long retryAfter = ttl != null && ttl > 0 ? ttl : OTP_SEND_WINDOW.toSeconds();
            return new Result(false, current, limit, retryAfter);
        }
        return new Result(true, current, limit, 0L);
    }

    public record Result(boolean allowed, long current, int limit, long retryAfterSeconds) {}
}
