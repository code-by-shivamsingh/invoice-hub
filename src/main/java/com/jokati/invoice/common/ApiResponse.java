
package com.jokati.invoice.common;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API response envelope used by all endpoints.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Schema(description = "Indicates whether the request was successfully processed.")
    private boolean success;

    @Schema(description = "HTTP status code (e.g., 200, 400, 404).")
    private int code;

    @Schema(description = "Human-readable message explaining the outcome.")
    private String message;

    @Schema(description = "Payload returned by the endpoint.")
    private T data;

    @Schema(description = "List of errors with code and message; empty when success=true.")
    private List<ErrorDetail> errors;

    @Schema(description = "RFC-3339 timestamp when the response was created.")
    private String timestamp;

    @Schema(description = "Per-request trace identifier.")
    private String traceId;

    @Schema(description = "Application name provided by the caller via 'X-Application-Name' header.")
    private String application;

    public ApiResponse() { }

    public ApiResponse(boolean success, int code, String message, T data, List<ErrorDetail> errors,
                       String timestamp, String traceId, String application) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.timestamp = timestamp;
        this.traceId = traceId;
        this.application = application;
    }

    // --- Static factories ---
    public static <T> ApiResponse<T> success(int code, String message, T data, String traceId, String application) {
        return new ApiResponse<>(
                true,
                code,
                message,
                data,
                Collections.emptyList(),
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                traceId,
                application
        );
    }

    public static <T> ApiResponse<T> failure(int code, String message, List<ErrorDetail> errors, String traceId, String application) {
        return new ApiResponse<>(
                false,
                code,
                message,
                null,
                errors != null ? errors : Collections.emptyList(),
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                traceId,
                application
        );
    }

    // --- Getters/Setters ---
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public List<ErrorDetail> getErrors() { return errors; }
    public void setErrors(List<ErrorDetail> errors) { this.errors = errors; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }
}

