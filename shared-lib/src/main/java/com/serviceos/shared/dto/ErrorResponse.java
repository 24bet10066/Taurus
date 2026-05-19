package com.serviceos.shared.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        int status,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors,
        Map<String, Object> details
) {
    public record FieldError(String field, String message, Object rejectedValue) {}

    public static ErrorResponse of(String code, String message, int status, String path) {
        return new ErrorResponse(code, message, status, path, Instant.now(), List.of(), Map.of());
    }

    public static ErrorResponse withFields(String code,
                                           String message,
                                           int status,
                                           String path,
                                           List<FieldError> fieldErrors) {
        return new ErrorResponse(code, message, status, path, Instant.now(), fieldErrors, Map.of());
    }
}
