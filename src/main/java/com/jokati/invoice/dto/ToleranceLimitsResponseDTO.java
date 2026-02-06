package com.jokati.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ToleranceLimitsResponseDTO {
    private String companyId;
    private BigDecimal freightCostsPercent;
    private BigDecimal standardAdditionalCostsPercent;
    private Boolean onlyPositiveDeviation;
    private List<ToleranceDTO> ancillaryTolerances;

}
