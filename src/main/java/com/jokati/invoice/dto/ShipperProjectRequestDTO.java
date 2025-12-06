
package com.jokati.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

/**
 * Payload for creating a project. Mirrors Node validation on userId and name.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperProjectRequestDTO {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "name is required")
    private String name;

    /** Additional dynamic fields, equivalent to Mongoose { strict: false } */
    private Map<String, Object> extra;
}
