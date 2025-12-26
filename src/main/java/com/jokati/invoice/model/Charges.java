
package com.jokati.invoice.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Charges {
    @DecimalMin("0.0") private BigDecimal freightCostSystemR;
    @DecimalMin("0.0") private BigDecimal ancillaryFee;
    @DecimalMin("0.0") private BigDecimal tollCostsR;
    @DecimalMin("0.0") private BigDecimal mobilityPackageR;
    @DecimalMin("0.0") private BigDecimal nonSortableGoods;
}