package com.jokati.invoice.dto;

import com.jokati.invoice.model.CarrierOffering;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CarrierOfferingResponseDTO {
    private String message;
    private CarrierOffering carrierOffering;
}