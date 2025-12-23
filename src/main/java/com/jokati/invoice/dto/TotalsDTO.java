
package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TotalsDTO {
    @JsonProperty("net_amount")   @NotNull @DecimalMin("0.0") private BigDecimal netAmount;
    @JsonProperty("vat_amount")   @NotNull @DecimalMin("0.0") private BigDecimal vatAmount;
    @JsonProperty("vat_type")     @NotBlank private String vatType;
    @JsonProperty("gross_amount") @NotNull @DecimalMin("0.0") private BigDecimal grossAmount;
}
