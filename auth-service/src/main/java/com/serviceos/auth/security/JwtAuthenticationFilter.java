package com.serviceos.auth.security;

import com.serviceos.auth.service.JwtService;
import com.serviceos.auth.service.TokenBlacklistService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklistService blacklist) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(BEARER.length());
        try {
            JwtService.Parsed parsed = jwtService.parse(token);
            if (blacklist.isRevoked(parsed.jti())) {
                chain.doFilter(request, response);
                return;
            }
            AuthenticatedUser principal = new AuthenticatedUser(
                    parsed.userId(),
                    parsed.phone(),
                    parsed.role(),
                    parsed.jti(),
                    parsed.expiresAt()
            );
            var authority = new SimpleGrantedAuthority("ROLE_" + parsed.role().name());
            var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
        }
        chain.doFilter(request, response);
    }
}
