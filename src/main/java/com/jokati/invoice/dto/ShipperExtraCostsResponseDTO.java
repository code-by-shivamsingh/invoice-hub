
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipperExtraCostsResponseDTO {
    private String message;
    private Object extraCosts;
}
