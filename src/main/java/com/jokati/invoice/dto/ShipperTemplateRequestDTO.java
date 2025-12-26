
package com.jokati.invoice.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors Node payload for ShipperTemplates;
 * keeps flexibility via Map for dynamic fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperTemplateRequestDTO {

    /** String representation of Mongo ObjectId to be used as _id */
    @NotBlank
    private String projectId;

    /** Templates are associated to a user */
    @NotBlank
    private String userId;

    /** Dynamic payload (Node used {strict:false}) */
    private Map<String, Object> template;
}
