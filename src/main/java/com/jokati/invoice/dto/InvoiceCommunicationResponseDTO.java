package com.jokati.invoice.dto;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvoiceCommunicationResponseDTO {
    private String invoiceNo;
    private String companyId;
    private String carrier;
    private String carrierEmail;

    private String createdById;
    private String createdByName;
    private String createdByType;

    private Instant createdAt;
    private Instant updatedAt;

    // Latest-first
    private List<InvoiceCommunicationMessageDTO> messages;
}