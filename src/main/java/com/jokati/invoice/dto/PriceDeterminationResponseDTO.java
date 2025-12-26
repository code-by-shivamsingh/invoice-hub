
package com.jokati.invoice.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriceDeterminationResponseDTO {
    private Map<String, Object> meta;
    private Map<String, Object> freightCalculationBasis; // ShipperFreightCalculationBasis.Countries
    private Map<String, Object> rates;                   // ShipperRates.rates
    private Map<String, Object> extraCosts;              // ShipperExtraCosts.extraCosts
    private List<Map<String, Object>> carrierData;       // Prepared offerings
    private Map<String, Object> dieselFloaterMatrix;     // DieselFloater.years
}
