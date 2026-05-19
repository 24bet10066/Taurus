package com.serviceos.gateway.filter;

import com.serviceos.gateway.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Validates JWT on every non-whitelisted request.
 * Injects X-User-Id and X-User-Role headers so downstream services can trust them.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";
    static final String ATTR_USER_ID = "gateway.userId";

    private static final Set<String> WHITELISTED_PATHS = Set.of(
            "/api/v1/auth/otp/send",
            "/api/v1/auth/otp/verify",
            "/api/v1/auth/refresh",
            "/api/v1/auth/health",
            "/actuator/health",
            "/actuator/info"
    );

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public int getOrder() {
        return -50;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Always pass OPTIONS (CORS preflight)
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = request.getPath().value();

        // Whitelisted paths — no auth
        if (WHITELISTED_PATHS.contains(path)
                || path.startsWith("/public/")
                || path.startsWith("/api/v1/payments/webhook/")) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER.length());
        try {
            JwtService.Parsed parsed = jwtService.parse(token);
            if (parsed.userId() == null || parsed.role() == null) {
                return unauthorized(exchange);
            }

            String userId = parsed.userId().toString();
            exchange.getAttributes().put(ATTR_USER_ID, userId);

            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", parsed.role())
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException ex) {
            log.debug("JWT validation failed for {}: {}", path, ex.getMessage());
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return exchange.getResponse().setComplete();
    }
}
