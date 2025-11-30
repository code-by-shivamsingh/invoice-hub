package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentResponseDTO {
    private String message;
    private int batchSize;
}
