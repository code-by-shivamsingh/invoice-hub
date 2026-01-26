package com.jokati.invoice.model;
import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tolerance {
	
    private String designation;

    @DecimalMin(value = "0.0", message = "Tolerance must be >= 0")
    @DecimalMax(value = "100.0", message = "Tolerance must be <= 100")
    private BigDecimal tolerancePercent;
}

