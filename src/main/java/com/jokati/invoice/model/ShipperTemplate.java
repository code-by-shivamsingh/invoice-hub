
package com.jokati.invoice.model;

import java.time.Instant;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
