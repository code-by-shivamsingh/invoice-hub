package com.jokati.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Send a message in invoice communication. Thread auto-creates if not exists.")
public class InvoiceCommunicationMessageRequestDTO {

    @NotBlank
    private String invoiceId;

    /**
     * Required only on first message (thread creation).
     * On replies, backend uses existing thread values if not provided.
     */
    private String companyId;
    private String carrier;
    private String carrierEmail;

    @NotBlank
    private String messageText;

    @NotBlank
    private String senderId;

    @NotBlank
    private String senderName;

    @NotBlank
    @Schema(description = "SHIPPER or CARRIER", example = "SHIPPER")
    private String senderType;
}