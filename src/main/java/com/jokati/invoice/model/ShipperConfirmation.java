
package com.jokati.invoice.model;

import java.util.List;
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
@Document(collection = "shipper-confirmation")
public class ShipperConfirmation {

    @Id
    private String id; // Mongo _id (equals shipperProjectId)

    private String shipperProjectId;
    private String shipperCompany;
    private String shipperProjectName;

    // mirrors confirmation._doc.selectedCarrierData (array of addresses)
    private List<Map<String, Object>> selectedCarrierData;

    // you can store other dynamic fields from payload here if needed
    private Map<String, Object> extra;
}
