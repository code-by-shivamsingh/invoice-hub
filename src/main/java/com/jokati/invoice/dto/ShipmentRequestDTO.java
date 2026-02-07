package com.jokati.invoice.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.jokati.invoice.util.TextSanitizer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String carrierProjectId; // <- top-level

    @NotNull
    @Size(min = 1)
    @Schema(description = "Shipment items (PascalCase) to save", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ShipmentItemRequestDTO> shipmentData;

    @Builder.Default
    @Schema(description = "Append (true) or replace (false) items", example = "true")
    private boolean append = true;

    // ✅ sanitize on JSON deserialization
    @JsonSetter("projectId")
    public void setProjectId(String projectId) {
        this.projectId = TextSanitizer.normalizeId(projectId);
    }

    @JsonSetter("carrierProjectId")
    public void setCarrierProjectId(String carrierProjectId) {
        this.carrierProjectId = TextSanitizer.normalizeId(carrierProjectId);
    }
}