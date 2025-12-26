
package com.jokati.invoice.dto;

import java.util.Map;

import lombok.Data;

@Data
public class CarrierExtraCostsRequestDTO {
    private String carrierProjectId;
    private Map<String, Object> extraCosts;
}
