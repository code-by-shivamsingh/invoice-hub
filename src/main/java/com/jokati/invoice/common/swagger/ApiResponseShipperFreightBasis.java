
package com.jokati.invoice.common.swagger;

import java.util.List;

import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.dto.ShipperFreightCalculationBasisResponseDTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiResponseShipperFreightBasis", description = "Envelope containing a freight calculation basis.")
public class ApiResponseShipperFreightBasis {

    @Schema(example = "true")
    public boolean success;

    @Schema(example = "200")
    public int code;

    @Schema(example = "Freight calculation basis fetched successfully")
    public String message;

    public ShipperFreightCalculationBasisResponseDTO data;

    public List<ErrorDetail> errors;

    @Schema(example = "2025-01-01T10:00:00Z")
    public String timestamp;

    @Schema(example = "b3f8e5b2")
    public String traceId;

    @Schema(example = "your-client-app")
    public String application;
}
