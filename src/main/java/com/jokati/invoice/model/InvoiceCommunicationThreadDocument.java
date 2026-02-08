package com.jokati.invoice.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "invoice-communications")
public class InvoiceCommunicationThreadDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String invoiceNo;

    // Header / participants
    private String companyId;      // shipper company/user id
    private String carrier;        // carrier name
    private String carrierEmail;   // optional

    // Creator info (thread header includes sender name)
    private String createdById;
    private String createdByName;
    private SenderType createdByType;

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Latest message first (index 0)
     */
    private List<Message> messages = new ArrayList<>();

    @Data
    public static class Message {
        private String messageId;   // UUID
        private String messageText;
        private Instant timestamp;

        private String senderId;
        private String senderName;
        private SenderType senderType;
    }

    public enum SenderType { SHIPPER, CARRIER }
}