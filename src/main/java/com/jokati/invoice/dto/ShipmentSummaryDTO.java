
package com.jokati.invoice.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShipmentSummaryDTO {
    @JsonProperty("total_shipments") @Min(0) private int totalShipments;
    @JsonProperty("total_weight_kg") @NotNull private BigDecimal totalWeightKg;
}
