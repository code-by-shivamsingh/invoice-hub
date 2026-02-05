package com.jokati.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateInvoiceCommunicationDTO {

    @NotBlank
    private String companyId;

    @NotBlank
    private String invoiceId;

    private String carrierEmail;  // optional
    @NotBlank
    private String carrier;       // required

    private String senderId;
    private String senderType;
    private String receiverType;
    private String message;
}

