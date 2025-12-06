
package com.jokati.invoice.model;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "shipper-freight-calculation-basis")
public class ShipperFreightCalculationBasis {

    /** MongoDB ObjectId */
    @Id
    private ObjectId id;

    /** Often same as id.toHexString(), but stored as String in your sample */
    private String projectId;

    /** e.g., 1 */
    private Integer carrierProjectId;

    /**
     * Capitalized key in Mongo document: "Countries".
     * Flexible nested structure: countryCode -> maps/options.
     */
    @Field("Countries")
    private Map<String, Object> countries;

    /** Firebase user id associated */
    private String firebaseId;

    /** Optional catch‑all for extra fields (e.g., __v or future attributes) */
    private Map<String, Object> extra;

    /** Auditing (requires @EnableMongoAuditing in your config) */
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
