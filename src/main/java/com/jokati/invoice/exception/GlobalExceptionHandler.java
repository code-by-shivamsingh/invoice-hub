
package com.jokati.invoice.exception;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ErrorCodes;
import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.common.ResponseUtil;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* -------------------- Validation errors -------------------- */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::toFieldErrorDetail)
                .collect(Collectors.toList());

        log.debug("Validation failed: {}", errors);
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBind(BindException ex) {
        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::toFieldErrorDetail)
                .collect(Collectors.toList());

        log.debug("Binding failed: {}", errors);
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorDetail> errors = ex.getConstraintViolations()
                .stream()
                .map(this::toConstraintErrorDetail)
                .collect(Collectors.toList());

        log.debug("Constraint violation: {}", errors);
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    private ErrorDetail toFieldErrorDetail(FieldError fe) {
        String rejected = safeRejectedValue(fe.getRejectedValue());
        return new ErrorDetail(
                ErrorCodes.VALIDATION_ERROR,
                fe.getDefaultMessage(),
                fe.getField(),
                rejected
        );
    }

    private ErrorDetail toConstraintErrorDetail(ConstraintViolation<?> cv) {
        String field = (cv.getPropertyPath() != null) ? cv.getPropertyPath().toString() : null;
        return new ErrorDetail(ErrorCodes.VALIDATION_ERROR, cv.getMessage(), field, null);
    }

    private String safeRejectedValue(Object rejectedValue) {
        if (rejectedValue == null) return null;
        String s;
        try {
            s = String.valueOf(rejectedValue);
        } catch (Exception ignore) {
            s = "<unprintable>";
        }
        // truncate very long values to avoid log noise
        return (s.length() > 512) ? s.substring(0, 512) + "…(truncated)" : s;
    }

    /* -------------------- Request parsing/type errors -------------------- */

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON body: {}", ex.getMessage());
        return ResponseUtil.error(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request body",
                new ErrorDetail(ErrorCodes.MALFORMED_JSON, "Malformed JSON request body")
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String field = ex.getName();
        String expected = (ex.getRequiredType() != null) ? ex.getRequiredType().getSimpleName() : "unknown";
        String provided = (ex.getValue() != null) ? ex.getValue().toString() : "null";
        return ResponseUtil.error(
                HttpStatus.BAD_REQUEST,
                "Type mismatch",
                new ErrorDetail(
                        ErrorCodes.TYPE_MISMATCH,
                        "Type mismatch for parameter '" + field + "'",
                        field,
                        "expected=" + expected + ", provided=" + provided
                )
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        String field = ex.getParameterName();
        return ResponseUtil.error(
                HttpStatus.BAD_REQUEST,
                "Missing request parameter",
                new ErrorDetail(ErrorCodes.MISSING_PARAM, "Missing request parameter '" + field + "'", field, null)
        );
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingPathVar(MissingPathVariableException ex) {
        String field = ex.getVariableName();
        return ResponseUtil.error(
                HttpStatus.BAD_REQUEST,
                "Missing path variable",
                new ErrorDetail(ErrorCodes.MISSING_PATH_VARIABLE, "Missing path variable '" + field + "'", field, null)
        );
    }

    /* -------------------- HTTP method / media type -------------------- */


@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
public ResponseEntity<ApiResponse<Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
    // getSupportedHttpMethods() may be null; be defensive
    var supportedMethods = ex.getSupportedHttpMethods();
    String detail = null;
    if (supportedMethods != null && !supportedMethods.isEmpty()) {
        detail = "Supported methods: " + supportedMethods.stream()
                .map(HttpMethod::name)   // <-- FIXED: use HttpMethod::name
                .collect(Collectors.joining(", "));
    }

    return ResponseUtil.error(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Method not allowed",
            new ErrorDetail(ErrorCodes.METHOD_NOT_ALLOWED, ex.getMessage(), null, detail)
    );
}


    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        List<String> supported = ex.getSupportedMediaTypes() != null
                ? ex.getSupportedMediaTypes().stream().map(MediaType::toString).collect(Collectors.toList())
                : List.of();
        String detail = supported.isEmpty() ? null : "Supported media types: " + String.join(", ", supported);

        return ResponseUtil.error(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type",
                new ErrorDetail(ErrorCodes.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), null, detail)
        );
    }

    /* -------------------- Domain errors -------------------- */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return ResponseUtil.error(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                new ErrorDetail(ErrorCodes.BAD_REQUEST, ex.getMessage())
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(NoSuchElementException ex) {
        log.debug("Not found: {}", ex.getMessage());
        return ResponseUtil.error(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                new ErrorDetail(ErrorCodes.NOT_FOUND, ex.getMessage())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        log.warn("Data integrity violation: {}", rootMsg);
        return ResponseUtil.error(
                HttpStatus.CONFLICT,
                "Data integrity violation",
                new ErrorDetail(ErrorCodes.CONFLICT, rootMsg)
        );
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateKey(DuplicateKeyException ex) {
        log.warn("Duplicate key: {}", ex.getMessage());
        return ResponseUtil.error(
                HttpStatus.CONFLICT,
                "Duplicate key",
                new ErrorDetail(ErrorCodes.CONFLICT, ex.getMessage())
        );
    }

    /* -------------------- Fallback -------------------- */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseUtil.error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error",
                new ErrorDetail(ErrorCodes.INTERNAL_ERROR, "Unexpected server error")
        );
    }
}
