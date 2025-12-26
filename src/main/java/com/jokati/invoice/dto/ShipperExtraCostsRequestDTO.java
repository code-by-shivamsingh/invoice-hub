
package com.jokati.invoice.dto;

import java.util.Map;

import lombok.Data;

@Data
public class ShipperExtraCostsRequestDTO {
    private String projectId;
    private Map<String, Object> extraCosts;
}
