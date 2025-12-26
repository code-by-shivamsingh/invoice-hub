
package com.jokati.invoice.common.swagger;

import java.util.List;

import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.dto.ShipperProjectResponseDTO;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Swagger-only helper to render ApiResponse<List<ShipperProjectResponseDTO>> nicely.
 * Not used at runtime; only for @Schema(implementation=...) in controller docs.
 */
@Schema(name = "ApiResponseListShipperProjectResponseDTO", description = "Envelope containing a list of shipper projects.")
public class ApiResponseListShipperProjectResponseDTO {

    @Schema(description = "Indicates whether the request was successfully processed.", example = "true")
    public boolean success;

    @Schema(description = "HTTP status code.", example = "200")
    public int code;

    @Schema(description = "Human-readable message.", example = "Projects fetched successfully")
    public String message;

    @ArraySchema(arraySchema = @Schema(description = "List of shipper projects"))
    public List<ShipperProjectResponseDTO> data;

    @ArraySchema(arraySchema = @Schema(description = "List of error items; empty on success"))
    public List<ErrorDetail> errors;

    @Schema(description = "Timestamp (RFC-3339).", example = "2025-01-01T10:00:00Z")
    public String timestamp;

    @Schema(description = "Trace id.", example = "b3f8e5b2")
    public String traceId;

    @Schema(description = "Application name provided via 'X-Application-Name'", example = "your-client-app")
    public String application;
}
