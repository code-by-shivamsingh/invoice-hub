
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CarrierOfferingResponseDTO {
    private String message;
    private Object data; // single doc or list, depending on endpoint
}
