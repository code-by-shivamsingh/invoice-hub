
package com.jokati.invoice.dto;

import java.util.Map;

import lombok.Data;

@Data
public class CarrierRatesRequestDTO {
    private String carrierProjectId;      // used as Mongo _id for POST/PUT upsert
    private String projectId;             // useful for GET and storing reference
    private Map<String, Object> rates;    // dynamic rate payload
    private Map<String, Object> payload;  // other dynamic fields (optional)
}
