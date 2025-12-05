
package com.jokati.invoice.model;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "shipper-templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipperTemplate {

    /** equals projectId in Node */
    @Id
    private ObjectId id;

    /** who owns this template */
    @Indexed
    private String userId;

    /** dynamic payload equivalent to {strict:false} */
    private Map<String, Object> template;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
