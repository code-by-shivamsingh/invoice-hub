
package com.jokati.invoice.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperProfileResponseDTO {
    private String id;                     // hex string of ObjectId (_id)
    private Map<String, Object> profile;   // dynamic content
    private Instant createdAt;
    private Instant updatedAt;
}
