package com.jokati.invoice.model;

import java.time.Instant;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
