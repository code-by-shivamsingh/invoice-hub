
package com.jokati.invoice.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChargesDTO {
    @JsonProperty("freightCostSystemR") @DecimalMin("0.0") private BigDecimal freightCostSystemR;
    @JsonProperty("ancillaryFee")        @DecimalMin("0.0") private BigDecimal ancillaryFee;
    @JsonProperty("tollCostsR")          @DecimalMin("0.0") private BigDecimal tollCostsR;
    @JsonProperty("mobilityPackageR")    @DecimalMin("0.0") private BigDecimal mobilityPackageR;
    @JsonProperty("nonSortableGoods")  @DecimalMin("0.0") private BigDecimal nonSortableGoods;
}


