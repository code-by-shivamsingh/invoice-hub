package com.jokati.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One update: set price for a specific weight and zone/zipCodeId.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperRateZonePriceUpdateItemDTO {

    @NotBlank(message = "weight is required (e.g. 100, 150)")
    private String weight;

    @NotBlank(message = "zipCodeId is required")
    private String zipCodeId;

    /**
     * Flexible input: can be Number or String (e.g. "29,1375").
     */
    @NotNull(message = "price is required")
    private Object price;
}