
package com.jokati.invoice.common.swagger;

import java.util.List;
import java.util.Map;

import com.jokati.invoice.common.ErrorDetail;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiResponseEmptyObject", description = "Envelope with an empty object as data.")
public class ApiResponseEmptyObject {

    @Schema(example = "true")
    public boolean success;

    @Schema(example = "200")
    public int code;

    @Schema(example = "OK")
    public String message;

    @Schema(description = "Empty object", example = "{}")
    public Map<String, Object> data;

    public List<ErrorDetail> errors;

    @Schema(example = "2025-01-01T10:00:00Z")
    public String timestamp;

    @Schema(example = "b3f8e5b2")
    public String traceId;

    @Schema(example = "your-client-app")
    public String application;
}
