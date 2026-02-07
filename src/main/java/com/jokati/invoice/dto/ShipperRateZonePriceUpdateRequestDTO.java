package com.jokati.invoice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update one or many zone (zipCodeId) prices inside one country for a project.
 * Only touches Prices[zipCodeId] for given weights.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperRateZonePriceUpdateRequestDTO {

    @NotBlank(message = "projectId is required")
    private String projectId;

    @NotBlank(message = "countryCode is required")
    private String countryCode;

    @NotEmpty(message = "updates must not be empty")
    @Valid
    private List<ShipperRateZonePriceUpdateItemDTO> updates;
}