package com.jokati.invoice.service;

import java.time.Instant;
import java.util.NoSuchElementException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.CreateInvoiceCommunicationDTO;
import com.jokati.invoice.model.InvoiceCarrierCommunication;
import com.jokati.invoice.model.InvoiceCarrierCommunication.CommunicationStatus;
import com.jokati.invoice.repository.InvoiceCarrierCommunicationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceCommunicationService {

    private final InvoiceCarrierCommunicationRepository repository;
    private final EmailService emailService;

    @Transactional
    public InvoiceCarrierCommunication create(CreateInvoiceCommunicationDTO request) {

        if (request.getInvoiceId() == null || request.getInvoiceId().isBlank()) {
            throw new IllegalArgumentException("invoiceId must not be blank");
        }
        if (request.getCarrierEmail() == null || request.getCarrierEmail().isBlank()) {
            throw new IllegalArgumentException("carrierEmail must not be blank");
        }

        if (repository.existsByInvoiceId(request.getInvoiceId())) {
            throw new DuplicateKeyException("Invoice communication already exists for invoiceId=" + request.getInvoiceId());
        }

        InvoiceCarrierCommunication entity = new InvoiceCarrierCommunication();
        entity.setInvoiceId(request.getInvoiceId());
        entity.setCarrierEmail(request.getCarrierEmail());
        entity.setShipperId(request.getShipperId());
        entity.setCarrierId(request.getCarrierId());
        entity.setCurrentStatus(CommunicationStatus.SENT);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        InvoiceCarrierCommunication.RequestInfo req = new InvoiceCarrierCommunication.RequestInfo();
        req.setSentAt(Instant.now());
        req.setMessage(request.getMessage());
        entity.setRequest(req);

        InvoiceCarrierCommunication saved = repository.save(entity);

        sendInvoiceEmail(saved);

        log.info("Invoice communication created: invoiceId={}", saved.getInvoiceId());
        return saved;
    }

    public InvoiceCarrierCommunication getByInvoiceId(String invoiceId) {
        return repository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Communication not found for invoiceId=" + invoiceId));
    }

    private void sendInvoiceEmail(InvoiceCarrierCommunication comm) {
        try {
            String invoiceLink = "http://localhost:8085/api/invoice-communications/" + comm.getInvoiceId();
            String html = """
                <html>
                  <body>
                    <p>Hello Carrier,</p>
                    <p>You have received a new invoice for review.</p>
                    <p>
                      <a href="%s">Click here to view the invoice</a>
                    </p>
                    <p><b>Invoice ID:</b> %s</p>
                  </body>
                </html>
                """.formatted(invoiceLink, comm.getInvoiceId());

            emailService.sendEmail(comm.getCarrierEmail(), "Invoice Review Request", html);

        } catch (Exception ex) {
            log.error("Failed to send invoice email: invoiceId={}", comm.getInvoiceId(), ex);
        }
    }
}
