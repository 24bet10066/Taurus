package com.serviceos.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceos.auth.dto.OtpSendRequest;
import com.serviceos.auth.dto.OtpVerifyRequest;
import com.serviceos.auth.enums.OtpPurpose;
import com.serviceos.auth.exception.GlobalExceptionHandler;
import com.serviceos.auth.repository.UserRepository;
import com.serviceos.auth.service.AuthService;
import com.serviceos.shared.enums.Role;
import com.serviceos.shared.result.AuthResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.serviceos.auth.security.JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @MockBean AuthService authService;
    @MockBean UserRepository userRepo;

    @Test
    void otpSend_returnsOk_whenPending() throws Exception {
        when(authService.sendOtp(any())).thenReturn(
                new AuthResult.OtpPending("9999999999", Instant.now().plusSeconds(600))
        );
        mvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new OtpSendRequest("9999999999", OtpPurpose.LOGIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("9999999999"));
    }

    @Test
    void otpSend_returns429_whenRateLimited() throws Exception {
        when(authService.sendOtp(any())).thenReturn(
                new AuthResult.Failure("RATE_LIMITED", "Too many OTP requests. Retry after 1800s")
        );
        mvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new OtpSendRequest("9999999999", OtpPurpose.LOGIN))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void otpVerify_returnsTokens_onSuccess() throws Exception {
        when(authService.verifyOtp(any())).thenReturn(
                new AuthResult.Success(UUID.randomUUID(), "9999999999", Role.CUSTOMER,
                        "access.jwt", "refresh.jwt", Instant.now().plusSeconds(86400))
        );
        mvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new OtpVerifyRequest("9999999999", "123456", OtpPurpose.LOGIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access.jwt"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh.jwt"));
    }

    @Test
    void otpVerify_returns401_onInvalidOtp() throws Exception {
        when(authService.verifyOtp(any())).thenReturn(
                new AuthResult.Failure("INVALID_OTP", "OTP does not match")
        );
        mvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new OtpVerifyRequest("9999999999", "000000", OtpPurpose.LOGIN))))
                .andExpect(status().isUnauthorized());
    }
}
