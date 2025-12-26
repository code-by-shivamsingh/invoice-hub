
package com.jokati.invoice.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for updating a project. Accepts either projectId or _id, and requires name.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperProjectUpdateRequestDTO {

    /** Either projectId or _id must be provided (hex string of ObjectId) */
    private String projectId;

    private String _id;

    @NotBlank(message = "name is required")
    private String name;

    /** Optional updates */
    private String userId;

    private Map<String, Object> extra;
}
