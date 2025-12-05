
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CarrierRatesResponseDTO {
    private String message;
    private Object carrierRates; // single doc or partial
}
