package com.jokati.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ToleranceLimitsPatchRequestDTO {

	private String companyId;

    @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal freightCostsPercent;

    @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal standardAdditionalCostsPercent;

    private Boolean onlyNegativeDeviation;


    @Valid
    private List<ToleranceDTO> ancillaryTolerances;
}
