package com.jokati.invoice.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Document(collection = "tolerance-limits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ToleranceLimits {
    @Id
    private String id;

    /** One entry per userId; unique index enforces this rule. */
    @Indexed(unique = true)
    private String companyId;

    @DecimalMin(value = "0.0", message = "Freight costs tolerance must be >= 0")
    @DecimalMax(value = "100.0", message = "Freight costs tolerance must be <= 100")
    private BigDecimal freightCostsPercent;

    @DecimalMin(value = "0.0", message = "Standard additional costs must be >= 0")
    @DecimalMax(value = "100.0", message = "Standard additional costs must be <= 100")
    private BigDecimal standardAdditionalCostsPercent;

    /** If true, only consider deviations with a negative difference. */
    private boolean onlyNegativeDeviation;

    @Valid
    @Builder.Default
    private List<Tolerance> ancillaryTolerances = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
