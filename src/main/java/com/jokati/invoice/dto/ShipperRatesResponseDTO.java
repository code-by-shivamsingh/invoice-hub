
package com.jokati.invoice.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipperRatesResponseDTO {
    private String id;            // Add id for clarity
    private String projectId;     // Project identifier
    private Map<String, Object> rates; // Rates data
}
