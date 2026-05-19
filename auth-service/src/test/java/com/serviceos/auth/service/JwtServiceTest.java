package com.serviceos.auth.service;

import com.serviceos.auth.config.AuthProperties;
import com.serviceos.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties(
                new AuthProperties.Jwt(
                        "test-secret-that-is-at-least-32-bytes-long-yes!",
                        Duration.ofDays(7),
                        Duration.ofDays(30),
                        "serviceos-auth-test"
                ),
                new AuthProperties.Otp(6, Duration.ofMinutes(10), 3, 5),
                new AuthProperties.TwoFactor("https://2factor.in/API/V1", "")
        );
        jwtService = new JwtService(props);
    }

    @Test
    void issuesAndParsesAccessToken() {
        UUID userId = UUID.randomUUID();
        JwtService.Issued issued = jwtService.issueAccessToken(userId, "9999999999", Role.CUSTOMER);

        JwtService.Parsed parsed = jwtService.parse(issued.token());

        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.phone()).isEqualTo("9999999999");
        assertThat(parsed.role()).isEqualTo(Role.CUSTOMER);
        assertThat(parsed.jti()).isEqualTo(issued.jti());
        assertThat(parsed.expiresAt()).isEqualTo(issued.expiresAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void issuedTokensHaveUniqueJti() {
        JwtService.Issued a = jwtService.issueAccessToken(UUID.randomUUID(), "1", Role.ADMIN);
        JwtService.Issued b = jwtService.issueAccessToken(UUID.randomUUID(), "2", Role.ADMIN);
        assertThat(a.jti()).isNotEqualTo(b.jti());
    }

    @Test
    void hashUtilIsDeterministic() {
        assertThat(HashUtil.sha256("123456")).isEqualTo(HashUtil.sha256("123456"));
        assertThat(HashUtil.sha256("123456")).isNotEqualTo(HashUtil.sha256("654321"));
        assertThat(HashUtil.sha256("123456")).hasSize(64);
    }
}
