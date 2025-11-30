
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipperRatesResponseDTO {
    private String message;
    private Object shipperRates;
}
