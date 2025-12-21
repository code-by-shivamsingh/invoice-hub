
package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Batch save shipment items for a project")
public class ShipmentRequestDTO {

    @NotBlank
    @Schema(example = "692af31934df801237c8fdda", requiredMode = Schema.RequiredMode.REQUIRED)
    private String projectId;

    @Schema(example = "692af2fe34df801237c8fdd1")
    private String carrierProjectId; // <- NEW: top-level

    @NotNull
    @Size(min = 1)
    @Schema(description = "Shipment items (PascalCase) to save", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ShipmentItemRequestDTO> shipmentData;

    @Builder.Default
    @Schema(description = "Append (true) or replace (false) items", example = "true")
    private boolean append = true;
}
