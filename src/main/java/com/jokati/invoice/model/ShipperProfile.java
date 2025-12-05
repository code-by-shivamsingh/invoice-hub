
package com.jokati.invoice.model;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "shipper-profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipperProfile {

    /** equals projectId in Node */
    @Id
    private ObjectId id;

    /** dynamic payload equivalent to {strict:false} */
    private Map<String, Object> profile;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
