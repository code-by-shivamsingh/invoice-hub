
package com.jokati.invoice.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;

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
