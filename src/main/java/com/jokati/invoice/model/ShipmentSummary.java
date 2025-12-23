
package com.jokati.invoice.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShipmentSummary {
    @Min(0) private int totalShipments;
    @NotNull private BigDecimal totalWeightKg;
}