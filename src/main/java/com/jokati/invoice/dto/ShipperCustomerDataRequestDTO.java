package com.jokati.invoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * Request payload for ShipperCustomerData.
 * Mirrors Node's flexible JSON (strict:false) by using Map<String, Object>.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperCustomerDataRequestDTO {
    @NotNull(message = "data payload must not be null")
    private Map<String, Object> data;
}
