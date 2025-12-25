
package com.jokati.invoice.exception;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.common.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> new ErrorDetail(
                        "VALIDATION_ERROR",
                        fe.getDefaultMessage(),
                        fe.getField(),
                        fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : null))
                .collect(Collectors.toList());

        log.debug("Validation failed: {}", errors);
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorDetail> errors = ex.getConstraintViolations()
                .stream()
                .map(this::toErrorDetail)
                .collect(Collectors.toList());

        log.debug("Constraint violation: {}", errors);
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    private ErrorDetail toErrorDetail(ConstraintViolation<?> cv) {
        String field = cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : null;
        return new ErrorDetail("VALIDATION_ERROR", cv.getMessage(), field, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Bad request: {}", ex.getMessage(), ex);
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, ex.getMessage(),
                new ErrorDetail("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
        log.debug("Not found: {}", ex.getMessage(), ex);
        return ResponseUtil.error(HttpStatus.NOT_FOUND, ex.getMessage(),
                new ErrorDetail("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON body", ex);
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, "Malformed JSON request body",
                new ErrorDetail("MALFORMED_JSON", "Malformed JSON request body"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        // Avoid leaking internals in message
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseUtil.error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error",
                new ErrorDetail("INTERNAL_ERROR", "Unexpected server error"));
    }
}
