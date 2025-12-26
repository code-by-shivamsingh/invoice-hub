package com.jokati.invoice.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
