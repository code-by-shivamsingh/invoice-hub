
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CarrierFreightCalculationBasisResponseDTO {
    private String message;
    private Object carrierFreightCalculationBasis; // full doc or partial (e.g., Countries)
}