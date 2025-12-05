
package com.jokati.invoice.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ShipperConfirmationRequestDTO {
    private Boolean sendToCarrier;
    private String shipperProjectId;    // used as Mongo _id
    private String shipperCompany;
    private String shipperProjectName;

    private List<CarrierAddressDTO> selectedCarrierData;

    // accept anything else (dynamic payload)
    private Map<String, Object> extra;
}
