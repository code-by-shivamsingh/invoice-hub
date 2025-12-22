package com.jokati.invoice.dto;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ToleranceDTO {
	@NotBlank
	private String designation;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal tolerancePercent;

}
