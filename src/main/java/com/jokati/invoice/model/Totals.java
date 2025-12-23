
package com.jokati.invoice.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Totals {
    @NotNull @DecimalMin("0.0") private BigDecimal netAmount;
    @NotNull @DecimalMin("0.0") private BigDecimal vatAmount;
    @NotBlank private String vatType;
    @NotNull @DecimalMin("0.0") private BigDecimal grossAmount;
}
