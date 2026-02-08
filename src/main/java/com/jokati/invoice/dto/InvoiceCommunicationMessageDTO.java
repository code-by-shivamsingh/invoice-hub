package com.jokati.invoice.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvoiceCommunicationMessageDTO {
    private String messageId;
    private String messageText;
    private Instant timestamp;

    private String senderId;
    private String senderName;
    private String senderType;
}