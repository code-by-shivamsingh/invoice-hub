package com.jokati.invoice.dto;


import lombok.Data;
import java.util.List;

@Data
public class ShipmentRequestDTO {
    private String projectId;
    private List<ShipmentDataDTO> shipmentData;
}
