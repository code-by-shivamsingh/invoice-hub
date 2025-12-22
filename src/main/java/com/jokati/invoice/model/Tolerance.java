package com.jokati.invoice.model;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tolerance {
	@NotBlank(message = "Designation is required")
    private String designation;

    @DecimalMin(value = "0.0", message = "Tolerance must be >= 0")
    @DecimalMax(value = "100.0", message = "Tolerance must be <= 100")
    private BigDecimal tolerancePercent;
}

