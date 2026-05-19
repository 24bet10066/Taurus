package com.serviceos.parts.exception;

import com.serviceos.shared.dto.ErrorResponse;
import com.serviceos.shared.exception.BusinessRuleViolationException;
import com.serviceos.shared.exception.CreditLimitExceededException;
import com.serviceos.shared.exception.InsufficientStockException;
import com.serviceos.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.withFields("VALIDATION_FAILED", "Request validation failed",
                        400, req.getRequestURI(), fields));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage(), 404, req.getRequestURI()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex,
                                                                 HttpServletRequest req) {
        var details = Map.<String, Object>of(
                "partId", ex.getPartId(),
                "sku", ex.getSku() != null ? ex.getSku() : "",
                "requested", ex.getRequested(),
                "available", ex.getAvailable()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("INSUFFICIENT_STOCK", ex.getMessage(), 422,
                        req.getRequestURI(), java.time.Instant.now(), List.of(), details));
    }

    @ExceptionHandler(CreditLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleCreditLimit(CreditLimitExceededException ex,
                                                           HttpServletRequest req) {
        var details = Map.<String, Object>of(
                "technicianId", ex.getTechnicianId(),
                "requested", ex.getRequested(),
                "available", ex.getAvailable(),
                "creditLimit", ex.getCreditLimit()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("CREDIT_LIMIT_EXCEEDED", ex.getMessage(), 422,
                        req.getRequestURI(), java.time.Instant.now(), List.of(), details));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessRuleViolationException ex,
                                                        HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), 422, req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("FORBIDDEN", "Access denied", 403, req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Unexpected server error", 500, req.getRequestURI()));
    }
}
