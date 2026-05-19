package com.serviceos.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * Sliding-window rate limiter using Redis INCR per IP per minute bucket.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    @Value("${serviceos.rate-limit.default-per-minute:100}")
    private int defaultLimit;

    @Value("${serviceos.rate-limit.public-per-minute:10}")
    private int publicLimit;

    private final ReactiveStringRedisTemplate redis;

    public RateLimitFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public int getOrder() {
        return -200;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        boolean isPublic = path.startsWith("/public/");
        int limit = isPublic ? publicLimit : defaultLimit;

        String ip = resolveClientIp(exchange);
        long bucket = System.currentTimeMillis() / 60_000L;
        String key = "ratelimit:" + ip + ":" + bucket;

        return redis.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        redis.expire(key, Duration.ofSeconds(90)).subscribe();
                    }
                    if (count > limit) {
                        log.warn("Rate limit exceeded for IP {} on {}", ip, path);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(limit));
                        exchange.getResponse().getHeaders().set("Retry-After", "60");
                        return exchange.getResponse().setComplete();
                    }
                    exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(limit));
                    exchange.getResponse().getHeaders().set("X-RateLimit-Remaining",
                            String.valueOf(Math.max(0, limit - count)));
                    return chain.filter(exchange);
                })
                .onErrorResume(ex -> {
                    // Redis unavailable — fail open (don't block requests)
                    log.warn("Rate limiter Redis error, failing open: {}", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remoteAddr = exchange.getRequest().getRemoteAddress();
        return remoteAddr != null ? remoteAddr.getAddress().getHostAddress() : "unknown";
    }
}
