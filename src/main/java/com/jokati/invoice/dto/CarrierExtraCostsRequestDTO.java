
package com.jokati.invoice.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CarrierExtraCostsRequestDTO {
    private String carrierProjectId;
    private Map<String, Object> extraCosts;
}
