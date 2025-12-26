

package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for Carrier Extra Costs.
 * Note: Message should ideally live in the envelope, not inside DTO.
 */
@Data
@Builder
public class CarrierExtraCostsResponseDTO {
    private Object carrierExtraCosts; // Carrier extra costs data
    private Object dieselFloater;     // Diesel Floater data
}
