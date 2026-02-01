package com.jokati.invoice.service;

import static com.jokati.invoice.constants.BillingStatusConstants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.model.Invoice;
import com.jokati.invoice.model.StatusInfo;
import com.jokati.invoice.repository.InvoiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ManualInvoiceDecisionService {

    private static final Logger log =
            LoggerFactory.getLogger(ManualInvoiceDecisionService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceStatusHistoryService historyService;

    /**
     * Manual decision using companyId + invoiceNumber
     */
    public void decideByInvoiceNumber(String companyId,
                                      String invoiceNumber,
                                      StatusInfo finalStatus,
                                      String userId,
                                      String role,
                                      String remark) {

        log.info("Manual decision started | companyId={} invoiceNumber={} status={}",
                companyId, invoiceNumber, finalStatus.getLabel());

        Invoice invoice = getInvoice(companyId, invoiceNumber);
        StatusInfo previousStatus = resolvePreviousStatus(invoice);

        applyInvoiceStatus(invoice, finalStatus);
        applyShipmentStatuses(invoice, finalStatus);

        invoiceRepository.save(invoice);

        saveHistory(invoice, previousStatus, finalStatus, userId, role, remark);

        log.info("Manual decision completed | invoiceId={} finalStatus={}",
                invoice.getId(), finalStatus.getLabel());
    }

    /**
     * Maps request status string to StatusInfo
     */
    public StatusInfo mapFinalStatus(String status) {

        if (MANUALLY_ACCEPTED.equalsIgnoreCase(status)) {
            return manualAcceptedStatus();
        }

        if (MANUALLY_REJECTED.equalsIgnoreCase(status)) {
            return manualRejectedStatus();
        }

        throw new IllegalArgumentException("Invalid decision status: " + status);
    }


    // private helper methods (professional structure)
    
    private Invoice getInvoice(String companyId, String invoiceNumber) {
        return invoiceRepository
                .findByCompanyIdAndInvoiceNumber(companyId, invoiceNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invoice not found for companyId=" + companyId +
                        ", invoiceNumber=" + invoiceNumber));
    }

    private StatusInfo resolvePreviousStatus(Invoice invoice) {
        return invoice.getFinalStatus() != null
                ? invoice.getFinalStatus()
                : invoice.getSystemStatus();
    }

    private void applyInvoiceStatus(Invoice invoice, StatusInfo finalStatus) {
        invoice.setFinalStatus(finalStatus);
    }

    private void applyShipmentStatuses(Invoice invoice, StatusInfo finalStatus) {

        if (invoice.getShipments() == null || invoice.getShipments().isEmpty()) {
            log.warn("No shipments found for invoiceId={}", invoice.getId());
            return;
        }

        invoice.getShipments().forEach(shipment ->
                shipment.setStatus(
                        StatusInfo.builder()
                                .label(finalStatus.getLabel())
                                .color(finalStatus.getColor())
                                .build()
                )
        );

        log.debug("All shipment statuses updated | invoiceId={} status={}",
                invoice.getId(), finalStatus.getLabel());
    }

    private void saveHistory(Invoice invoice,
                             StatusInfo previousStatus,
                             StatusInfo finalStatus,
                             String userId,
                             String role,
                             String remark) {

        historyService.saveManual(
                invoice.getId(),
                previousStatus != null ? previousStatus.getLabel() : null,
                finalStatus.getLabel(),
                userId,
                role,
                remark
        );
    }
}
