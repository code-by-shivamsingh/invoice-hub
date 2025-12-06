
package com.jokati.invoice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "carrier-offering")
public class CarrierOffering {

    @Id
    private String id;                 // equals carrierProjectId (Mongo _id)

    private String carrierProjectId;   // also stored for filtering or reference
    private String projectId;          // used in GET query to fetch offerings by shipper project
    private String shipperEmail;       // destination email for notification

    // Keep dynamic payload like Mongoose { strict:false }
    private Map<String, Object> companyProfile; // contains { company, ... }
    private Map<String, Object> payload; // store remaining fields if desired
    private Map<String, Object> freightCalculationBasis;
    private Map<String, Object> extraCosts;
    private Map<String, Object> rates;

}
