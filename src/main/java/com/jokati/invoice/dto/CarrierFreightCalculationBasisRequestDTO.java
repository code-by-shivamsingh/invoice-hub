
package com.jokati.invoice.dto;

import java.util.Map;

import lombok.Data;

@Data
public class CarrierFreightCalculationBasisRequestDTO {
    private String carrierProjectId;         // used as Mongo _id
    private Map<String, Object> countries;   // mirrors "Countries" document field
    // private Map<String, Object> extra;    // optional for dynamic spillover
}
