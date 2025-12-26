
package com.jokati.invoice.common.swagger;

import java.util.List;

import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.dto.ShipperExtraCostsResponseDTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiResponseShipperExtraCosts", description = "Envelope containing shipper extra costs.")
public class ApiResponseShipperExtraCosts {
    public boolean success;
    public int code;
    public String message;
    public ShipperExtraCostsResponseDTO data;
    public List<ErrorDetail> errors;
    public String timestamp;
    public String traceId;
    public String application;
}
