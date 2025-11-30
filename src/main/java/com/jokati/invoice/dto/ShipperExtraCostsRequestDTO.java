
package com.jokati.invoice.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ShipperExtraCostsRequestDTO {
    private String projectId;
    private Map<String, Object> extraCosts;
}
