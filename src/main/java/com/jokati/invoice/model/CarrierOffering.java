
package com.jokati.invoice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "carrier-offering")
public class CarrierOffering {

    @Id
    private String id;                 // mirrors Mongo _id (Node sets to ObjectId of carrierProjectId)

    @Indexed
    private String carrierProjectId;   // filter key in upsert query

    @Indexed
    private String projectId;          // GET query filter

    private String shipperEmail;

    // These fields are object-shaped; ensure DB stores {} when empty (not [])
    @Field("companyProfile")
    private Map<String, Object> companyProfile;

    @Field("payload")
    private Map<String, Object> payload;

    @Field("freightCalculationBasis")
    private Map<String, Object> freightCalculationBasis;

    @Field("extraCosts")
    private Map<String, Object> extraCosts;

    @Field("rates")
    private Map<String, Object> rates;
}
