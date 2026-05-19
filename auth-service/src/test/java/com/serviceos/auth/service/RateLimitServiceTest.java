package com.serviceos.auth.service;

import com.serviceos.auth.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {

    private StringRedisTemplate redis;
    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> ops;
    private RateLimitService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        AuthProperties props = new AuthProperties(
                new AuthProperties.Jwt("x", Duration.ofDays(7), Duration.ofDays(30), "iss"),
                new AuthProperties.Otp(6, Duration.ofMinutes(10), 3, 5),
                new AuthProperties.TwoFactor("https://2factor.in/API/V1", "")
        );
        service = new RateLimitService(redis, props);
    }

    @Test
    void allowsUnderLimit() {
        when(ops.increment(anyString())).thenReturn(1L);
        RateLimitService.Result r = service.tryRegisterOtpSend("9999999999");
        assertThat(r.allowed()).isTrue();
        assertThat(r.current()).isEqualTo(1L);
    }

    @Test
    void blocksOverLimit() {
        when(ops.increment(anyString())).thenReturn(6L);
        when(redis.getExpire(anyString())).thenReturn(1800L);
        RateLimitService.Result r = service.tryRegisterOtpSend("9999999999");
        assertThat(r.allowed()).isFalse();
        assertThat(r.retryAfterSeconds()).isEqualTo(1800L);
    }
}
