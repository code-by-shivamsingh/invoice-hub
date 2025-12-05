
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CarrierConfirmationResponseDTO {
    private String message;
    private Object data; // the saved/loaded document
}
