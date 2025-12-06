
package com.jokati.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * Incoming payload for ShipperFreightCalculationBasis create/update.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperFreightCalculationBasisRequestDTO {

    @NotBlank(message = "projectId is required")
    private String projectId;

    /** optional; if your flow sets this */
    private Integer carrierProjectId;

    /** Flexible nested structure for "Countries" */
    @NotNull(message = "countries must not be null")
    private Map<String, Object> countries;

    /** optional */
    private String firebaseId;

    /** optional dynamic additions */
    private Map<String, Object> extra;
}
