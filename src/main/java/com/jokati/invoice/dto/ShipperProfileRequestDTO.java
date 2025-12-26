
package com.jokati.invoice.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors Node payload for ShipperProfile; keeps it flexible via Map.
 * Must contain projectId.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperProfileRequestDTO {

    /** String representation of Mongo ObjectId to be used as _id */
    @NotBlank
    private String projectId;

    /** Dynamic payload (Node used {strict:false}) */
    private Map<String, Object> profile;
}
