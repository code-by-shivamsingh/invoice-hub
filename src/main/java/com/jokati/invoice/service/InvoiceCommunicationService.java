package com.jokati.invoice.service;

import java.time.Instant;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.CreateInvoiceCommunicationDTO;
import com.jokati.invoice.model.InvoiceCarrierCommunication;
import com.jokati.invoice.repository.InvoiceCarrierCommunicationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceCommunicationService {

    private final InvoiceCarrierCommunicationRepository repository;
    private static final Logger log = LoggerFactory.getLogger(InvoiceCommunicationService.class);

    @Transactional
    public InvoiceCarrierCommunication create(CreateInvoiceCommunicationDTO request) {

        if (request.getCompanyId() == null || request.getCompanyId().isBlank()) {
            throw new IllegalArgumentException("companyId must not be blank");
        }
        if (request.getInvoiceId() == null || request.getInvoiceId().isBlank()) {
            throw new IllegalArgumentException("invoiceId must not be blank");
        }
        if (request.getCarrier() == null || request.getCarrier().isBlank()) {
            throw new IllegalArgumentException("carrier name must not be blank");
        }

        if (repository.existsByInvoiceId(request.getInvoiceId())) {
            throw new DuplicateKeyException("Invoice communication already exists for invoiceId=" + request.getInvoiceId());
        }

        InvoiceCarrierCommunication entity = new InvoiceCarrierCommunication();
        entity.setCompanyId(request.getCompanyId());
        entity.setInvoiceId(request.getInvoiceId());
        entity.setCarrier(request.getCarrier());
        entity.setCarrierEmail(request.getCarrierEmail());  // optional
        entity.setShipperId(request.getSenderId());
        entity.setCurrentStatus(InvoiceCarrierCommunication.CommunicationStatus.SENT);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        InvoiceCarrierCommunication.RequestInfo req = new InvoiceCarrierCommunication.RequestInfo();
        req.setSentAt(Instant.now());
        req.setMessage(request.getMessage());
        entity.setRequest(req);

        InvoiceCarrierCommunication saved = repository.save(entity);

        // Send email only if carrierEmail is provided
        if (saved.getCarrierEmail() != null && !saved.getCarrierEmail().isBlank()) {
            sendInvoiceEmail(saved);
        }

        return saved;
    }

    public InvoiceCarrierCommunication getByInvoiceId(String invoiceId) {
        return repository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Communication not found for invoiceId=" + invoiceId));
    }

    private void sendInvoiceEmail(InvoiceCarrierCommunication comm) {
        String invoiceLink = "http://localhost:8085/api/v1/invoice-communications/" + comm.getInvoiceId();
        String html = """
                <html>
                  <body>
                    <p>Hello %s,</p>
                    <p>You have received a new invoice for review.</p>
                    <p><a href="%s">Click here to view the invoice</a></p>
                    <p><b>Invoice ID:</b> %s</p>
                  </body>
                </html>
                """.formatted(comm.getCarrier(), invoiceLink, comm.getInvoiceId());

        log.info("Email sent to {}: \n{}", comm.getCarrierEmail(), html);
    }
}
