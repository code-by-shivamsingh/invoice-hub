
package com.jokati.invoice.dto;

import java.util.Map;

import lombok.Data;

@Data
public class ShipperRatesRequestDTO {
    private String projectId;
    private Map<String, Object> rates;
}
