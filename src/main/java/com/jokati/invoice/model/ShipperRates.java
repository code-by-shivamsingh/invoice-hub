
package com.jokati.invoice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "shipper-rates")
public class ShipperRates {
    @Id
    private String id;
    private String projectId;
    private Map<String, Object> rates; // Flexible structure for dynamic fields
}
