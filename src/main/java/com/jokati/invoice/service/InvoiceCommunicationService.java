package com.jokati.invoice.service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.InvoiceCommunicationMessageDTO;
import com.jokati.invoice.dto.InvoiceCommunicationMessageRequestDTO;
import com.jokati.invoice.dto.InvoiceCommunicationResponseDTO;
import com.jokati.invoice.model.InvoiceCommunicationThreadDocument;
import com.jokati.invoice.model.InvoiceCommunicationThreadDocument.Message;
import com.jokati.invoice.model.InvoiceCommunicationThreadDocument.SenderType;
import com.jokati.invoice.repository.InvoiceCommunicationThreadRepository;
import com.jokati.invoice.util.TextSanitizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceCommunicationService {

    private final InvoiceCommunicationThreadRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Single POST behavior:
     * - If thread doesn't exist: auto-create (requires companyId + carrier; carrierEmail optional)
     * - Always adds message at index 0 (latest-first)
     * - If senderType == SHIPPER: send email to carrier (if carrierEmail exists)
     */
    @Transactional
    public InvoiceCommunicationResponseDTO sendMessage(InvoiceCommunicationMessageRequestDTO request) {

        final String invoiceId = TextSanitizer.normalizeId(request.getInvoiceId());
        final String senderId  = TextSanitizer.normalizeId(request.getSenderId());
        final String senderName = TextSanitizer.trimUnicode(request.getSenderName());

        if (invoiceId == null || invoiceId.isBlank()) throw new IllegalArgumentException("invoiceId must not be blank");
        if (request.getMessageText() == null || request.getMessageText().isBlank()) throw new IllegalArgumentException("messageText must not be blank");
        if (senderId == null || senderId.isBlank()) throw new IllegalArgumentException("senderId must not be blank");
        if (senderName == null || senderName.isBlank()) throw new IllegalArgumentException("senderName must not be blank");

        SenderType senderType;
        try {
            senderType = SenderType.valueOf(request.getSenderType().trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid senderType: " + request.getSenderType() + " (expected SHIPPER/CARRIER)");
        }

        // Build message
        Message msg = new Message();
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setText(request.getMessageText().trim());
        msg.setTimestamp(Instant.now());
        msg.setSenderId(senderId);
        msg.setSenderName(senderName);
        msg.setSenderType(senderType);

        boolean exists = repository.existsByInvoiceId(invoiceId);

        // If thread doesn't exist, it MUST be created (auto-create on shipper first message)
        // We enforce required fields on first create.
        String companyId = TextSanitizer.normalizeId(request.getCompanyId());
        String carrier   = TextSanitizer.trimUnicode(request.getCarrier());
        String carrierEmail = TextSanitizer.trimUnicode(request.getCarrierEmail());

        if (!exists) {
            // Auto-create only when shipper sends first message (your rule)
            if (senderType != SenderType.SHIPPER) {
                throw new NoSuchElementException("Thread not found for invoiceId=" + invoiceId + ". Shipper must initiate the thread first.");
            }
            if (companyId == null || companyId.isBlank()) throw new IllegalArgumentException("companyId is required for first message");
            if (carrier == null || carrier.isBlank()) throw new IllegalArgumentException("carrier is required for first message");

            log.info("InvoiceCommunication AUTO-CREATE: invoiceId={}, companyId={}, carrier={}, senderId={}, senderName={}",
                    invoiceId, companyId, carrier, senderId, senderName);
        }

        log.info("InvoiceCommunication SEND: invoiceId={}, senderType={}, senderId={}, msgId={}, threadExists={}",
                invoiceId, senderType, senderId, msg.getMessageId(), exists);

        // Atomic upsert + push message (latest first)
        Instant now = Instant.now();
        Query q = Query.query(Criteria.where("invoiceId").is(invoiceId));

        Update u = new Update()
                .set("updatedAt", now)
                .push("messages").atPosition(0).value(msg);

        // Only set these on insert (first create)
        if (!exists) {
            u.setOnInsert("invoiceId", invoiceId)
             .setOnInsert("companyId", companyId)
             .setOnInsert("carrier", carrier)
             .setOnInsert("carrierEmail", carrierEmail)
             .setOnInsert("createdById", senderId)
             .setOnInsert("createdByName", senderName)
             .setOnInsert("createdByType", senderType)
             .setOnInsert("createdAt", now);
        } else {
            // If exists and request provides carrierEmail (optional), you may update it
            if (carrierEmail != null && !carrierEmail.isBlank()) {
                u.set("carrierEmail", carrierEmail);
            }
        }

        mongoTemplate.upsert(q, u, InvoiceCommunicationThreadDocument.class);

        // Fetch updated thread
        InvoiceCommunicationThreadDocument doc = repository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Invoice communication not found for invoiceId=" + invoiceId));

        // Email: shipper -> carrier
        if (senderType == SenderType.SHIPPER) {
            String to = doc.getCarrierEmail();
            if (to != null && !to.isBlank()) {
                sendCarrierEmail(doc, msg);
            } else {
                log.warn("InvoiceCommunication EMAIL skipped: carrierEmail empty for invoiceId={}", invoiceId);
            }
        }

        return toResponseDTO(doc);
    }

    public InvoiceCommunicationResponseDTO getByInvoiceId(String invoiceIdPath) {
        final String invoiceId = TextSanitizer.normalizeId(invoiceIdPath);
        log.info("InvoiceCommunication GET: invoiceId={}", invoiceId);

        InvoiceCommunicationThreadDocument doc = repository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Invoice communication not found for invoiceId=" + invoiceId));

        return toResponseDTO(doc);
    }

    private InvoiceCommunicationResponseDTO toResponseDTO(InvoiceCommunicationThreadDocument doc) {
        List<InvoiceCommunicationMessageDTO> msgs = (doc.getMessages() == null ? List.<Message>of() : doc.getMessages())
                .stream()
                .map(m -> InvoiceCommunicationMessageDTO.builder()
                        .messageId(m.getMessageId())
                        .text(m.getText())
                        .timestamp(m.getTimestamp())
                        .senderId(m.getSenderId())
                        .senderName(m.getSenderName())
                        .senderType(m.getSenderType() == null ? null : m.getSenderType().name())
                        .build())
                .toList();

        return InvoiceCommunicationResponseDTO.builder()
                .invoiceId(doc.getInvoiceId())
                .companyId(doc.getCompanyId())
                .carrier(doc.getCarrier())
                .carrierEmail(doc.getCarrierEmail())
                .createdById(doc.getCreatedById())
                .createdByName(doc.getCreatedByName())
                .createdByType(doc.getCreatedByType() == null ? null : doc.getCreatedByType().name())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .messages(msgs) // already latest-first in DB
                .build();
    }

    /**
     * Production note:
     * Replace this stub with your real EmailService integration.
     */
    private void sendCarrierEmail(InvoiceCommunicationThreadDocument thread, Message msg) {
        String link = "http://localhost:8085/api/v1/invoice-communications/" + thread.getInvoiceId();

        String html = """
                <html>
                  <body>
                    <p>Hello %s,</p>
                    <p>You have received a new message regarding invoice <b>%s</b>.</p>
                    <p><b>From:</b> %s</p>
                    <p><b>Message:</b> %s</p>
                    <p><a href="%s">Click here to open the conversation</a></p>
                  </body>
                </html>
                """.formatted(
                thread.getCarrier(),
                thread.getInvoiceId(),
                msg.getSenderName(),
                escapeHtml(msg.getText()),
                link
        );

        log.info("EMAIL (stub) to={} invoiceId={} html={}", thread.getCarrierEmail(), thread.getInvoiceId(), html);
    }

    // Minimal HTML escaping (avoid breaking HTML)
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}