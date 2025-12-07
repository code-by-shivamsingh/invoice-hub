
package com.jokati.invoice.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CarrierOfferingRequestDTO {
    private String projectId;
    private String carrierProjectId;
    private String shipperEmail;

    private Map<String, Object> companyProfile;
    private Map<String, Object> payload;
    private Map<String, Object> freightCalculationBasis;
    private Map<String, Object> extraCosts;
    private Map<String, Object> rates;
}


