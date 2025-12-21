
package com.jokati.invoice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "shipment-data")
public class ProjectShipmentDocument {

    @Id
    private String id;

    @Field("projectId")
    private String projectId;

    @Field("carrierProjectId")
    private String carrierProjectId; // <- NEW: mirror top-level carrierProjectId

    @Field("shipmentData")
    private List<ShipmentItemDocument> shipmentData;

    @Field("createdAt")
    private Instant createdAt;
}
