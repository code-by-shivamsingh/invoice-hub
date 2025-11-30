
package com.jokati.invoice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "carrier-extra-costs")
public class CarrierExtraCosts {
    @Id
    private String id;
    private String carrierProjectId;
    private Map<String, Object> extraCosts; // Flexible structure like strict:false in Mongoose
}
