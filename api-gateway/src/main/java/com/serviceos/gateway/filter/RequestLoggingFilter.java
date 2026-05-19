package com.serviceos.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Logs method, path, userId, response status, and duration for every request.
 * Runs outermost so doFinally executes after all other filters complete.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getPath().value();

        return chain.filter(exchange)
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - start;
                    String userId = exchange.getAttributes()
                            .getOrDefault(JwtAuthenticationFilter.ATTR_USER_ID, "anonymous").toString();
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;
                    log.info("{} {} userId={} status={} {}ms", method, path, userId, status, duration);
                });
    }
}
