
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
@Document(collection = "carrier-freight-calculation-basis")
public class CarrierFreightCalculationBasis {

    @Id
    private String id; // Mongo _id (equals carrierProjectId)
    
    @Indexed // Optional: speeds up queries by projectId
    private String projectId;

    /**
     * Node returns result.Countries (capitalized).
     * We keep Java field 'countries' but map it to Mongo field "Countries".
     */

    @Field("Countries")
    private Map<String, Object> countries;

    // If you store other dynamic fields in payload, you can add:
    // private Map<String, Object> extra;
}
