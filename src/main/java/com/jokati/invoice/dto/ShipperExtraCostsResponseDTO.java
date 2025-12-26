
package com.jokati.invoice.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipperExtraCostsResponseDTO {
    private String id;
    private String projectId;
    private Map<String, Object> extraCosts; // keep flexible as per your entity
}
