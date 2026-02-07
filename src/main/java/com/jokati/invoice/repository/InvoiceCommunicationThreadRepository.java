package com.jokati.invoice.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.InvoiceCommunicationThreadDocument;

public interface InvoiceCommunicationThreadRepository
        extends MongoRepository<InvoiceCommunicationThreadDocument, String> {

    Optional<InvoiceCommunicationThreadDocument> findByInvoiceId(String invoiceId);

    boolean existsByInvoiceId(String invoiceId);
}