package com.jokati.invoice.dto;

import com.jokati.invoice.model.InvoiceCarrierCommunication.CommunicationStatus;
import lombok.Data;

@Data
public class CarrierResponseDTO {
    private CommunicationStatus status; 
    private String comment;
}
