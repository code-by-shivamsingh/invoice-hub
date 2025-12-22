package com.jokati.invoice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ToleranceLimitsResponseDTO {

private String id;
    private String userId;
    private BigDecimal freightCostsPercent;
    private BigDecimal standardAdditionalCostsPercent;
    private boolean onlyNegativeDeviation;
    private List<ToleranceDTO> ancillaryTolerances;
    private Instant createdAt;
    private Instant updatedAt;

}
