
package com.jokati.invoice.model;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "shipper-projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipperProject {

    @Id
    private ObjectId id;

    @Indexed
    private String userId;

    @Indexed
    private String name;
    
    private String street;
    private String streetNo;
    private String zipCode;
    private String city;
    private String country;
    private String contactName;
    private Double phoneNo;
    private String email;

    /** Dynamic fields equivalent to { strict: false } in Mongoose */
    private Map<String, Object> extra;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
