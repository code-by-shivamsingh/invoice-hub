
package com.jokati.invoice.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class CarrierConfirmationRequestDTO {
    private Boolean sendToCarrier;
    private String carrierProjectId;  // used as Mongo _id
    private String shipperProjectId;
    private String shipperCompany;
    private String shipperProjectName;

    // mirrors result._doc.selectedCarrierData in Node
    private List<CarrierAddressDTO> selectedCarrierData;

    // accept anything else (dynamic payload)
    private Map<String, Object> extra;
}
