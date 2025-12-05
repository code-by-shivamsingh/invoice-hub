
package com.jokati.invoice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "carrier-rates")
public class CarrierRates {

    @Id
    private String id;              // Mongo _id (equals carrierProjectId or projectId per Node usage)

    private String carrierProjectId; // stored for clarity/upserts
    private String projectId;        // used by GET endpoint (Node queries by projectId)

    // Flexible payload structure (Mongoose strict:false equivalent)
    private Map<String, Object> rates;
    private Map<String, Object> payload; // optional: store any additional request fields
}
