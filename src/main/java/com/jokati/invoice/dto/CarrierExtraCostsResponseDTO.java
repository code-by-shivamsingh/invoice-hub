
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CarrierExtraCostsResponseDTO {
    private String message;
    private Object carrierExtraCosts;
    private Object dieselFloater; // Added for DieselFloater data
}
