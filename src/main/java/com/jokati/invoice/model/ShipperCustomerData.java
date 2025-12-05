package com.jokati.invoice.model;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "shipper-customer-data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipperCustomerData {

    @Id
    private ObjectId id;

    /**
     * Dynamic payload container (equivalent to Mongoose {strict:false}).
     */
    private Map<String, Object> data;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
