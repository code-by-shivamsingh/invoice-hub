package com.jokati.invoice.dto;


import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ToleranceDTO {
	
	private String designation;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal tolerancePercent;

}
