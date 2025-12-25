
package com.jokati.invoice.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed error item used by the response envelope.")
public class ErrorDetail {

    @Schema(description = "Application or domain specific error code (e.g., VALIDATION_ERROR, NOT_FOUND).")
    private String code;

    @Schema(description = "Human-readable error message.")
    private String message;

    @Schema(description = "Optional field name associated with the error.")
    private String field;

    @Schema(description = "Optional extra details.")
    private String details;

    public ErrorDetail() { }

    public ErrorDetail(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorDetail(String code, String message, String field, String details) {
        this.code = code;
        this.message = message;
        this.field = field;
        this.details = details;
    }

    // Getters/Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
