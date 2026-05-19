package com.serviceos.gateway.filter;

import com.serviceos.gateway.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest {

    @Mock JwtService jwtService;
    @Mock GatewayFilterChain chain;

    @InjectMocks JwtAuthenticationFilter filter;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void whitelistedPath_noJwt_passes() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/otp/send").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        verify(jwtService, never()).parse(anyString());
    }

    @Test
    void optionsRequest_passes_withoutJwt() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/v1/jobs/123").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtService, never()).parse(anyString());
    }

    @Test
    void missingAuthHeader_returns401() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/jobs").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void invalidJwt_returns401() {
        when(jwtService.parse("bad_token")).thenThrow(new JwtException("invalid"));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/jobs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad_token")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validJwt_addsUserHeadersAndContinues() {
        when(jwtService.parse("valid_token"))
                .thenReturn(new JwtService.Parsed(userId, "ADMIN"));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/analytics/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid_token")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(argThat(ex -> {
            String xUserId = ex.getRequest().getHeaders().getFirst("X-User-Id");
            String xRole = ex.getRequest().getHeaders().getFirst("X-User-Role");
            return userId.toString().equals(xUserId) && "ADMIN".equals(xRole);
        }));
    }

    @Test
    void publicPath_noJwt_passes() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/public/request").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtService, never()).parse(anyString());
        verify(chain).filter(any());
    }

    @Test
    void actuatorHealth_noJwt_passes() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
