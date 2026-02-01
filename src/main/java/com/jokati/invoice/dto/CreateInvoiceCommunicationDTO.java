package com.jokati.invoice.dto;

import lombok.Data;

@Data
public class CreateInvoiceCommunicationDTO {
    private String invoiceId;
    private String carrierEmail;
    private String shipperId;
    private String carrierId;
    private String message;
}
