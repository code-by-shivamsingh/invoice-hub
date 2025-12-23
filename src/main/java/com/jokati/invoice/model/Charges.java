
package com.jokati.invoice.model;

import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Charges {
    @DecimalMin("0.0") private BigDecimal freightCostSystemR;
    @DecimalMin("0.0") private BigDecimal ancillaryFee;
    @DecimalMin("0.0") private BigDecimal tollCostsR;
    @DecimalMin("0.0") private BigDecimal mobilityPackageR;
    @DecimalMin("0.0") private BigDecimal nonSortableGoods;
}