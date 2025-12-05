
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipperConfirmationResponseDTO {
    private String message;
    private Boolean success;
    private Object confirmation; // saved/loaded document
    private Object error;        // optional error details
}
