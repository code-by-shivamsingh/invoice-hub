
package com.jokati.invoice.common;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public final class ResponseUtil {

    private ResponseUtil() {}

    public static String currentTraceId() {
        String id = MDC.get("traceId");
        return (id != null && !id.isBlank()) ? id : "unknown";
    }

    public static String currentApplication() {
        String app = MDC.get("application");
        return (app != null && !app.isBlank()) ? app : null;
    }

    // 200 OK
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), message, data, currentTraceId(), currentApplication())
        );
    }

    // For custom status
    public static <T> ResponseEntity<ApiResponse<T>> withStatus(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status)
                .body(ApiResponse.success(status.value(), message, data, currentTraceId(), currentApplication()));
    }

    // Errors
    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message, List<ErrorDetail> errors) {
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(status.value(), message, errors, currentTraceId(), currentApplication()));
    }

    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message, ErrorDetail error) {
        return error(status, message, List.of(error));
    }
}
