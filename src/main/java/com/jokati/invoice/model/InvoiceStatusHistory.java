package com.jokati.invoice.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "invoice-status-history")
@Getter
@Setter        
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceStatusHistory {

    @Id
    private String id;

    private String invoiceId;
    private String previousStatus;
    private String newStatus;

    private String updatedBy;
    private String updatedByRole;
    private String remark;

    private Instant updatedAt;
}
