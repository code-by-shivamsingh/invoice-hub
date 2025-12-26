
package com.jokati.invoice.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Totals {
    @NotNull @DecimalMin("0.0") private BigDecimal netAmount;
    @NotNull @DecimalMin("0.0") private BigDecimal vatAmount;
    @NotBlank private String vatType;
    @NotNull @DecimalMin("0.0") private BigDecimal grossAmount;
}
