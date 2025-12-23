
package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChargesDTO {
    @JsonProperty("freightCostSystemR") @DecimalMin("0.0") private BigDecimal freightCostSystemR;
    @JsonProperty("ancillaryFee")        @DecimalMin("0.0") private BigDecimal ancillaryFee;
    @JsonProperty("tollCostsR")          @DecimalMin("0.0") private BigDecimal tollCostsR;
    @JsonProperty("mobilityPackageR")    @DecimalMin("0.0") private BigDecimal mobilityPackageR;
    @JsonProperty("nonSortableGoods")  @DecimalMin("0.0") private BigDecimal nonSortableGoods;
}


