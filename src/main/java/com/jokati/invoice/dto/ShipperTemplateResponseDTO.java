
package com.jokati.invoice.dto;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperTemplateResponseDTO {
    private String id;                    // hex string of ObjectId (_id)
    private String userId;                // owner
    private Map<String, Object> template; // dynamic content
    private Instant createdAt;
    private Instant updatedAt;
}
