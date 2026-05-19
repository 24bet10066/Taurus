package com.serviceos.auth.controller;

import com.serviceos.auth.dto.CreateTechnicianRequest;
import com.serviceos.auth.service.AuthService;
import com.serviceos.shared.dto.ApiResponse;
import com.serviceos.shared.dto.ErrorResponse;
import com.serviceos.shared.result.AuthResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/admin")
@Tag(name = "auth-admin", description = "Admin-only auth operations")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Create a TECHNICIAN_HIRED or TECHNICIAN_FREE account")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create-technician")
    public ResponseEntity<?> createTechnician(@Valid @RequestBody CreateTechnicianRequest req) {
        AuthResult result = authService.createTechnician(req);
        return switch (result) {
            case AuthResult.Success s -> ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.ok(Map.of(
                            "userId", s.userId(),
                            "phone", s.phone(),
                            "role", s.role()
                    ), "technician created")
            );
            case AuthResult.Failure f when "PHONE_EXISTS".equals(f.code()) ->
                    ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(ErrorResponse.of(f.code(), f.message(), 409, "/api/v1/auth/admin/create-technician"));
            case AuthResult.Failure f ->
                    ResponseEntity.badRequest()
                            .body(ErrorResponse.of(f.code(), f.message(), 400, "/api/v1/auth/admin/create-technician"));
            case AuthResult.OtpPending p ->
                    ResponseEntity.internalServerError()
                            .body(ErrorResponse.of("UNEXPECTED", "create-technician returned OtpPending",
                                    500, "/api/v1/auth/admin/create-technician"));
        };
    }
}
