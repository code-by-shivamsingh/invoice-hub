package com.jokati.invoice.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.InvoiceStatusHistory;

public interface InvoiceStatusHistoryRepository
        extends MongoRepository<InvoiceStatusHistory, String> {

    List<InvoiceStatusHistory> findByInvoiceIdOrderByUpdatedAtAsc(String invoiceId);
}
