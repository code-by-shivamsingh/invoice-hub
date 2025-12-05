
package com.jokati.invoice.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CarrierFreightCalculationBasisRequestDTO {
    private String carrierProjectId;         // used as Mongo _id
    private Map<String, Object> countries;   // mirrors "Countries" document field
    // private Map<String, Object> extra;    // optional for dynamic spillover
}
