package com.jokati.invoice.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "invoice_carrier_communication")
public class InvoiceCarrierCommunication {

    @Id
    private String id;

   
    @Indexed(unique = true)
    private String invoiceId;

    private String carrierEmail;

    // Request info (send time)
    private RequestInfo request;

    // Carrier response info 
    private ResponseInfo response;

    // SENT / MANUALLY_ACCEPTED / MANUALLY_REJECTED
    private String currentStatus;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class RequestInfo {
        private Instant sentAt;
        private String message;
    }

    @Data
    public static class ResponseInfo {
        private String status;        // MANUALLY_ACCEPTED / MANUALLY_REJECTED
        private String comment;
        private Instant respondedAt;
    }
}
