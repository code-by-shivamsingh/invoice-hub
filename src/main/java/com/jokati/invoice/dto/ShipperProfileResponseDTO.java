
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
public class ShipperProfileResponseDTO {
    private String id;                     // hex string of ObjectId (_id)
    private Map<String, Object> profile;   // dynamic content
    private Instant createdAt;
    private Instant updatedAt;
}
