package com.jokati.invoice.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.InvoiceCarrierCommunication;

public interface InvoiceCarrierCommunicationRepository
        extends MongoRepository<InvoiceCarrierCommunication, String> {

    Optional<InvoiceCarrierCommunication> findByInvoiceId(String invoiceId);

    boolean existsByInvoiceId(String invoiceId);
}
