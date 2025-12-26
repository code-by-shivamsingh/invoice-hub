
package com.jokati.invoice.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
