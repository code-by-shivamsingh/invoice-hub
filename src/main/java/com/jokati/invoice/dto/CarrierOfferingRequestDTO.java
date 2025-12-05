
package com.jokati.invoice.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CarrierOfferingRequestDTO {
    private String carrierProjectId;          // used as Mongo _id
    private String projectId;                 // for GET queries
    private String shipperEmail;              // notification recipient
    private Map<String, Object> companyProfile; // expects key "company"
    private Map<String, Object> payload;        // other dynamic fields from request
}
