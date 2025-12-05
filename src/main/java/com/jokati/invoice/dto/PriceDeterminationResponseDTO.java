
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

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
