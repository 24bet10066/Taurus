package com.serviceos.auth.controller;

import com.serviceos.auth.dto.AccessTokenResponse;
import com.serviceos.auth.dto.MeResponse;
import com.serviceos.auth.dto.OtpSendRequest;
import com.serviceos.auth.dto.OtpSendResponse;
import com.serviceos.auth.dto.OtpVerifyRequest;
import com.serviceos.auth.dto.RefreshRequest;
import com.serviceos.auth.dto.TokenResponse;
import com.serviceos.auth.entity.User;
import com.serviceos.auth.repository.UserRepository;
import com.serviceos.auth.security.AuthenticatedUser;
import com.serviceos.auth.service.AuthService;
import com.serviceos.shared.dto.ApiResponse;
import com.serviceos.shared.dto.ErrorResponse;
import com.serviceos.shared.exception.ResourceNotFoundException;
import com.serviceos.shared.result.AuthResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "auth", description = "OTP login, JWT, refresh, logout")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepo;

    public AuthController(AuthService authService, UserRepository userRepo) {
        this.authService = authService;
        this.userRepo = userRepo;
    }

    @Operation(summary = "Send OTP to phone (2Factor.in)")
    @PostMapping("/otp/send")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody OtpSendRequest req) {
        AuthResult result = authService.sendOtp(req);
        return switch (result) {
            case AuthResult.OtpPending p -> ResponseEntity.ok(
                    ApiResponse.ok(new OtpSendResponse(p.phone(), p.expiresAt(), "OTP sent"))
            );
            case AuthResult.Failure f when "RATE_LIMITED".equals(f.code()) ->
                    ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .header("Retry-After", retryAfterFromMessage(f.message()))
                            .body(ErrorResponse.of(f.code(), f.message(), 429, "/api/v1/auth/otp/send"));
            case AuthResult.Failure f ->
                    ResponseEntity.badRequest()
                            .body(ErrorResponse.of(f.code(), f.message(), 400, "/api/v1/auth/otp/send"));
            case AuthResult.Success s -> ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("UNEXPECTED", "send returned Success", 500, "/api/v1/auth/otp/send"));
        };
    }

    @Operation(summary = "Verify OTP and issue tokens")
    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        AuthResult result = authService.verifyOtp(req);
        return switch (result) {
            case AuthResult.Success s -> ResponseEntity.ok(
                    ApiResponse.ok(new TokenResponse(
                            s.userId(), s.phone(), s.role(),
                            s.accessToken(), s.refreshToken(), s.expiresAt()))
            );
            case AuthResult.Failure f when "USER_BLOCKED".equals(f.code()) ->
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ErrorResponse.of(f.code(), f.message(), 403, "/api/v1/auth/otp/verify"));
            case AuthResult.Failure f when "TOO_MANY_ATTEMPTS".equals(f.code()) ->
                    ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(ErrorResponse.of(f.code(), f.message(), 429, "/api/v1/auth/otp/verify"));
            case AuthResult.Failure f ->
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ErrorResponse.of(f.code(), f.message(), 401, "/api/v1/auth/otp/verify"));
            case AuthResult.OtpPending p -> ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("UNEXPECTED", "verify returned OtpPending", 500, "/api/v1/auth/otp/verify"));
        };
    }

    @Operation(summary = "Rotate refresh token and issue new access token")
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest req) {
        AuthResult result = authService.refresh(req.refreshToken());
        return switch (result) {
            case AuthResult.Success s -> ResponseEntity.ok(
                    ApiResponse.ok(new AccessTokenResponse(s.accessToken(), s.refreshToken(), s.expiresAt()))
            );
            case AuthResult.Failure f -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(f.code(), f.message(), 401, "/api/v1/auth/refresh"));
            case AuthResult.OtpPending p -> ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("UNEXPECTED", "refresh returned OtpPending", 500, "/api/v1/auth/refresh"));
        };
    }

    @Operation(summary = "Revoke current access token and all refresh tokens")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.logout(principal.userId(), principal.jti(), principal.expiresAt());
        return ResponseEntity.ok(ApiResponse.ok(null, "logged out"));
    }

    @Operation(summary = "Current authenticated user")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User u = userRepo.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.userId()));
        return ResponseEntity.ok(ApiResponse.ok(new MeResponse(
                u.getId(), u.getPhone(), u.getName(), u.getEmail(),
                u.getRole(), u.isActive(), u.getCreatedAt())));
    }

    private static String retryAfterFromMessage(String message) {
        int i = message.indexOf("after ");
        if (i < 0) return "3600";
        int j = message.indexOf("s", i);
        if (j < 0) return "3600";
        try {
            return Integer.parseInt(message.substring(i + 6, j).trim()) + "";
        } catch (NumberFormatException ignored) {
            return "3600";
        }
    }
}
