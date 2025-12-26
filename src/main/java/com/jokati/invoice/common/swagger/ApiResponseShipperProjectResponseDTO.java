
package com.jokati.invoice.common.swagger;

import java.util.List;

import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.dto.ShipperProjectResponseDTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiResponseShipperProjectResponseDTO", description = "Envelope containing a shipper project.")
public class ApiResponseShipperProjectResponseDTO {

    @Schema(example = "true")
    public boolean success;

    @Schema(example = "200")
    public int code;

    @Schema(example = "Project fetched successfully")
    public String message;

    public ShipperProjectResponseDTO data;

    public List<ErrorDetail> errors;

    @Schema(example = "2025-01-01T10:00:00Z")
    public String timestamp;

    @Schema(example = "b3f8e5b2")
    public String traceId;

    @Schema(example = "your-client-app")
    public String application;
}
