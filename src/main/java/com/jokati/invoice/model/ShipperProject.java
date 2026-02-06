
package com.jokati.invoice.model;

import java.time.Instant;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "shipper-projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipperProject {

    @Id
    private ObjectId id;

    /** Tenant/company identifier (same as userId/companyId for now) */
    @Indexed
    private String userId;
    @Indexed
    private String companyId;

    /** Project display name — we will match this to the carrier name case-insensitively */
    @Indexed
    private String name;

    private String street;
    private String streetNo;
    private String zipCode;
    private String city;
    private String country;
    private String contactName;
    private String phoneNo;
    private String customerNumber;
    private String email;
    private Boolean active;

    /** Dynamic fields equivalent to { strict: false } in Mongoose */
    private Map<String, Object> extra;  // ✅ fixed HTML entity

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
