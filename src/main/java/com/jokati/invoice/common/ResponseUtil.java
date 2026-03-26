
package com.jokati.invoice.common;

import java.util.Collections;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Response builder utilities for the standard ApiResponse envelope.
 * - Adds X-Trace-Id and X-App-Name headers automatically when available in MDC.
 * - Provides strongly-typed and Object-typed helpers.
 * - Keeps Node-style {} responses for "empty" success payloads.
 */
public final class ResponseUtil {

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_APPLICATION = "application";

    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_APP_NAME = "X-App-Name";

    private ResponseUtil() {}

    /** Reads trace id from MDC; returns null if absent (headers will still include it when set). */
    public static String currentTraceId() {
        String id = MDC.get(MDC_TRACE_ID);
        return (id != null && !id.isBlank()) ? id : null;
    }

    /** Reads application name from MDC; returns null if absent. */
    public static String currentApplication() {
        String app = MDC.get(MDC_APPLICATION);
        return (app != null && !app.isBlank()) ? app : null;
    }

    /* ----------------- Internal helpers ----------------- */

    private static HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String traceId = currentTraceId();
        String app = currentApplication();
        if (traceId != null) headers.add(HEADER_TRACE_ID, traceId);
        if (app != null) headers.add(HEADER_APP_NAME, app);
        return headers;
    }

    private static <T> ApiResponse<T> successBody(int code, String message, T data) {
        return ApiResponse.success(code, message, data, currentTraceId(), currentApplication());
    }

    private static ApiResponse<Object> failureBody(int code, String message, List<ErrorDetail> errors) {
        return ApiResponse.failure(code, message, errors != null ? errors : List.of(), currentTraceId(), currentApplication());
    }

    /* ----------------- Strongly typed helpers (existing, refined) ----------------- */

    /** 200 OK with typed data. */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok()
                .headers(defaultHeaders())
                .body(successBody(HttpStatus.OK.value(), message, data));
    }

    /** Custom status with typed data. */
    public static <T> ResponseEntity<ApiResponse<T>> withStatus(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status)
                .headers(defaultHeaders())
                .body(successBody(status.value(), message, data));
    }

    /** 201 Created with typed data. */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(defaultHeaders())
                .body(successBody(HttpStatus.CREATED.value(), message, data));
    }

    /** 204 No Content with envelope (no data). */
    public static ResponseEntity<ApiResponse<Object>> noContent(String message) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .headers(defaultHeaders())
                .body(successBody(HttpStatus.NO_CONTENT.value(), message, null));
    }

    /** Error response with list of ErrorDetail. */
    public static ResponseEntity<ApiResponse<Object>> error(HttpStatus status, String message, List<ErrorDetail> errors) {
        return ResponseEntity.status(status)
                .headers(defaultHeaders())
                .body(failureBody(status.value(), message, errors));
    }

    /** Error response with single ErrorDetail. */
    public static ResponseEntity<ApiResponse<Object>> error(HttpStatus status, String message, ErrorDetail error) {
        return error(status, message, error != null ? List.of(error) : List.of());
    }

    /** Error response with varargs ErrorDetail for convenience. */
    public static ResponseEntity<ApiResponse<Object>> error(HttpStatus status, String message, ErrorDetail... errors) {
        return error(status, message, errors != null ? List.of(errors) : List.of());
    }

    /* ----------------- Object-typed convenience helpers ----------------- */

    /** 200 OK with arbitrary data (envelope uses T=Object). */
    public static ResponseEntity<ApiResponse<Object>> okObject(Object data, String message) {
        return ResponseEntity.ok()
                .headers(defaultHeaders())
                .body(successBody(HttpStatus.OK.value(), message, data));
    }

    /** 200 OK with empty object {} as data (Node-style). */
    public static ResponseEntity<ApiResponse<Object>> okEmpty(String message) {
        return ResponseEntity.ok()
                .headers(defaultHeaders())
                .body(successBody(HttpStatus.OK.value(), message, Collections.emptyMap()));
    }
    
    /** ✅ NEW: 200 OK with message only (data = null) */
    public static ResponseEntity<ApiResponse<Object>> ok(String message) {
        return ResponseEntity.ok()
                .headers(defaultHeaders())
                .body(successBody(HttpStatus.OK.value(), message, null));
    }

    /** Custom status with arbitrary data (envelope uses T=Object). */
    public static ResponseEntity<ApiResponse<Object>> withStatusObject(HttpStatus status, String message, Object data) {
        return ResponseEntity.status(status)
                .headers(defaultHeaders())
                .body(successBody(status.value(), message, data));
    }

    /** Custom status with empty object {} as data. */
    public static ResponseEntity<ApiResponse<Object>> withStatusEmpty(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .headers(defaultHeaders())
                .body(successBody(status.value(), message, Collections.emptyMap()));
    }
}
