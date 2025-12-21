
package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentSaveResponseDTO {

    @JsonProperty("shipmentData")
    private List<ShipmentItemResponseDTO> shipmentData;
}
