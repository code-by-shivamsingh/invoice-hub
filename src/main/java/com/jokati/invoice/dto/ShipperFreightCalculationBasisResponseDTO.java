
package com.jokati.invoice.dto;

import lombok.*;
import java.time.Instant;
import java.util.Map;

/**
 * Strongly-typed response DTO for ShipperFreightCalculationBasis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperFreightCalculationBasisResponseDTO {

    private String message;

    /** Hex string of Mongo ObjectId */
    private String id;

    private String projectId;

    private Integer carrierProjectId;

    private Map<String, Object> countries;

    private String firebaseId;

    private Map<String, Object> extra;

    private Instant createdAt;
    private Instant updatedAt;
}
