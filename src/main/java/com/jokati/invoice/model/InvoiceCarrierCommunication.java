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
    private String shipperId;
    private String carrierId;
    private String carrierEmail;

    private RequestInfo request;

    private CommunicationStatus currentStatus; 

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class RequestInfo {
        private Instant sentAt;
        private String message;
    }

    
    public static enum CommunicationStatus {
        SENT
    }
}
