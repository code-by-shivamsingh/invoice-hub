
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CarrierTemplatesResponseDTO {
    private String message;
    private Object data; // single doc or result info
}
