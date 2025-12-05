
package com.jokati.invoice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "carrier-confirmation")
public class CarrierConfirmation {

    @Id
    private String id; // Mongo _id: equals carrierProjectId

    // Flexible payload structure (strict:false in Mongoose)
    private String shipperProjectId;
    private String shipperCompany;
    private String shipperProjectName;

    // The request contains selectedCarrierData (array of addresses)
    private List<Map<String, Object>> selectedCarrierData;

    // Any additional dynamic fields from payload
    private Map<String, Object> extra; // optional to store spillover
}
