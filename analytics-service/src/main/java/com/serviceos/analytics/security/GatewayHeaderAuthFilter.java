package com.serviceos.analytics.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads X-User-Id and X-User-Role headers set by the API gateway.
 * No JWT validation — the gateway is trusted for intra-service calls.
 */
@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");

        if (userId != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var authority = new SimpleGrantedAuthority("ROLE_" + role);
            var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}
