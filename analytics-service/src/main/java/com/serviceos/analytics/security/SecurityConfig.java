package com.serviceos.analytics.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final GatewayHeaderAuthFilter gatewayFilter;

    public SecurityConfig(GatewayHeaderAuthFilter gatewayFilter) {
        this.gatewayFilter = gatewayFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/actuator/health", "/actuator/info",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/v1/analytics/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
