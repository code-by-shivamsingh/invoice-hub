
package com.jokati.invoice.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CarrierRatesRequestDTO {
    private String carrierProjectId;      // used as Mongo _id for POST/PUT upsert
    private String projectId;             // useful for GET and storing reference
    private Map<String, Object> rates;    // dynamic rate payload
    private Map<String, Object> payload;  // other dynamic fields (optional)
}
