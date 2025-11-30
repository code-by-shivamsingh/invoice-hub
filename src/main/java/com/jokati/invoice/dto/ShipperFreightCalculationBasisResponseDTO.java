
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipperFreightCalculationBasisResponseDTO {
    private String message;
    private Object shipperFreightCalculationBasis;
}
