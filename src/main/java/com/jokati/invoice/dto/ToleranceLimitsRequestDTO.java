package com.jokati.invoice.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ToleranceLimitsRequestDTO {

	@NotBlank
    private String companyId;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal freightCostsPercent;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal standardAdditionalCostsPercent;

    private boolean onlyPositiveDeviation;

    @Valid
    private List<ToleranceDTO> ancillaryTolerances;
}


