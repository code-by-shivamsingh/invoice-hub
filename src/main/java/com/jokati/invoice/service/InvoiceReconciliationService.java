
package com.jokati.invoice.service;

import com.jokati.invoice.dto.ToleranceLimitsResponseDTO;
import com.jokati.invoice.model.Invoice;
import com.jokati.invoice.model.Shipment;
import com.jokati.invoice.model.ShipmentItemDocument;
import com.jokati.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.jokati.invoice.service.ShipperProjectService;

/**
 * Orchestrates invoice & shipment reconciliation:
 *  - Resolve project by carrier
 *  - Load shipment data & compute shipment order totals
 *  - Roll-up invoice order total, difference, and percent difference
 *  - Apply tolerance and set invoice status
 *  - Trigger finance emails for accepted cases
 *  - Persist enriched invoice document
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceReconciliationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ShipperProjectService shipperProjectService;
    private final ShipmentService shipmentService;
    private final ToleranceLimitsService toleranceService;
    private final EmailService emailService;
    private final InvoiceRepository invoiceRepository;

    // Email templates & recipients (configure in application.yml)
    @Value("${finance.email:finance@yourcompany.com}")
    private String financeEmail;

    @Value("${email.templates.accepted:invoice_accepted}")
    private String templateAccepted;

    @Value("${email.templates.toleranceAccepted:invoice_tolerance_accepted}")
    private String templateToleranceAccepted;

    /**
     * Entry point to reconcile & persist an invoice and all its shipments.
     *
     * @param invoice   the invoice entity (already built from request DTO)
     * @return the persisted, enriched invoice (with order totals, differences, statuses, and emailSent)
     * @throws Exception for missing critical data or service failures
     */
    public Invoice reconcileAndPersist(Invoice invoice) throws Exception {
      log.info("Reconciling invoice. companyId={}, invoiceNumber={}, carrier={}",
              invoice.getCompanyId(),
              safe(invoice.getInvoiceNumber()),
              invoice.getSeller() != null ? safe(invoice.getSeller().getCompanyName()) : "N/A");

      validateInvoiceBasics(invoice);

      // Step 1: get projectId from ShipperProjects service using carrier name
      final String companyId = invoice.getCompanyId();
      final String carrierName = invoice.getSeller().getCompanyName();
      ObjectId projectId = shipperProjectService.getProjectIdByCarrier(companyId, carrierName);
      log.info("Resolved projectId={} for companyId={} and carrierName={}", projectId.toHexString(), companyId, carrierName);

      // Step 2 & 3 & 4: For each shipment, load shipment data + compute orderTotal/difference/status
      if (invoice.getShipments() != null) {
          for (Shipment s : invoice.getShipments()) {
              enrichShipmentOrderTotals(s, projectId, companyId);
          }
      } else {
          log.warn("Invoice has no shipments. companyId={}, invoiceNumber={}", companyId, invoice.getInvoiceNumber());
      }

      // Step 5 & 6: Roll-up invoice totals & persist invoice-level difference/status
      rollupInvoiceTotalsAndDifference(invoice);

      // Step 7: Load tolerance limits
      var toleranceLimit = toleranceService.getByCompanyId(companyId); // Must expose getFreightCostsPercent()
      log.info("Loaded tolerance for companyId={}: freightCostsPercent={}",
              companyId, toleranceLimit != null ? toleranceLimit.getFreightCostsPercent() : null);

      // Step 8 & 9: Decide invoice status, compute percent diff, and handle email notifications
      decideInvoiceStatusAndNotify(invoice, toleranceLimit);

      // Persist
      Invoice saved = invoiceRepository.save(invoice);
      log.info("Invoice reconciliation complete & persisted. id={}, status={}, emailSent={}, orderTotal={}, invoiceDifference={}",
              saved.getId(), saved.getStatus(), saved.getEmailStatus(), saved.getOrderTotal(), saved.getInvoiceDifference());

      return saved;
    }

    // ---------------------- Shipment-level enrichment ----------------------

    /**
     * Enrich a single shipment:
     *  - Call ShipmentService to get shipment data for (shipmentId, projectId)
     *  - Compute orderTotal = freight_cost + extra_cost  [TODO: replace dummy logic]
     *  - Compute difference = orderTotal - net_amount_eur
     *  - Set shipment status
     */
    private void enrichShipmentOrderTotals(Shipment shipment, ObjectId projectId, String companyId) {
        log.debug("Enriching shipment. shipmentId={}, projectId={}, companyId={}",
                safe(shipment.getShipmentId()), projectId != null ? projectId.toHexString() : "null", companyId);

        // Step 2: get shipment data from internal ShipmentService

        List<ShipmentItemDocument> linesForShipment =
        shipmentService.getShipmentsByShipmentIdAndProjectId(projectId, shipment.getShipmentId());
        log.debug("all shipments {}:",linesForShipment);

        // Step 3: business logic placeholder
        // TODO: Replace with real computation from 'data' when available:
        //  - freight_cost = derive from shipmentData (e.g., base rate, zone, weight, etc.)
        //  - extra_cost   = sum of ancillary surcharges from shipmentData
       
        BigDecimal freightCost = calculateFreightCost(null);; // add data
        BigDecimal extraCost =  calculateExtraCost(null);;    // add data
        
       /*  incase shipment data service give me  freightCost aand extraCost the use them*/

//        if (data != null) {
//            freightCost = data.getFreightCost(); // TODO ensure shipmentData exposes freightCost
//            extraCost   = data.getExtraCost();   // TODO ensure shipmentData exposes extraCost
//        } else {
            log.warn("ShipmentService returned null data. shipmentId={}, projectId={}", shipment.getShipmentId(), projectId);
       // }

        BigDecimal orderTotal = safeAdd(freightCost, extraCost);

        shipment.setOrderTotal(orderTotal);
        log.debug("Shipment orderTotal computed. shipmentId={}, freightCost={}, extraCost={}, orderTotal={}",
                shipment.getShipmentId(), freightCost, extraCost, orderTotal);

        // Step 4: difference between orderTotal and net_amount_eur from invoice request
        if (orderTotal == null || shipment.getNetAmountEur() == null) {
            shipment.setDifference(null);
            shipment.setStatus("Incorrect billing");
            log.warn("Missing values for difference calc. shipmentId={}, orderTotal={}, netAmountEur={}",
                    shipment.getShipmentId(), orderTotal, shipment.getNetAmountEur());
        } else {
            BigDecimal diff = orderTotal.subtract(shipment.getNetAmountEur());
            shipment.setDifference(diff);
            shipment.setStatus(diff.compareTo(BigDecimal.ZERO) == 0 ? "Correct billing" : "Incorrect billing");
            log.debug("Shipment difference computed. shipmentId={}, difference={}, status={}",
                    shipment.getShipmentId(), diff, shipment.getStatus());
        }
    }

    // ---------------------- Invoice roll-up ----------------------

    private BigDecimal calculateExtraCost(ShipmentItemDocument data) {
		// TODO Auto-generated method stub
		return null;
	}

	private BigDecimal calculateFreightCost(ShipmentItemDocument data) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
     * Step 5 & 6:
     *  - invoice.orderTotal = sum of shipments' orderTotal
     *  - invoice.invoiceDifference = orderTotal - totals.grossAmount
     *  - persist fields on Invoice entity
     */
    private void rollupInvoiceTotalsAndDifference(Invoice invoice) {
        BigDecimal sumOrderTotal = BigDecimal.ZERO;
        boolean anyOrderPresent = false;

        if (invoice.getShipments() != null) {
            for (Shipment s : invoice.getShipments()) {
                if (s.getOrderTotal() != null) {
                    sumOrderTotal = sumOrderTotal.add(s.getOrderTotal());
                    anyOrderPresent = true;
                }
            }
        }

        // If none had orderTotal, keep invoice.orderTotal = null to reflect missing data
        invoice.setOrderTotal(anyOrderPresent ? sumOrderTotal : null);

        BigDecimal invTotal = (invoice.getTotals() != null) ? invoice.getTotals().getGrossAmount() : null;
        BigDecimal invoiceDiff = null;

        if (invoice.getOrderTotal() != null && invTotal != null) {
            invoiceDiff = invoice.getOrderTotal().subtract(invTotal);
        }

        invoice.setInvoiceDifference(invoiceDiff);

        log.info("Invoice roll-up done. invoiceNumber={}, orderTotal={}, invoiceTotal={}, invoiceDifference={}",
                safe(invoice.getInvoiceNumber()), invoice.getOrderTotal(), invTotal, invoiceDiff);
    }

    // ---------------------- Status + Tolerance + Email ----------------------

    /**
     * Step 7, 8, 9:
     *  - Load tolerance (already passed in)
     *  - Compute percent difference
     *  - Decide status:
     *      Accepted:           difference == 0 → email finance
     *      Tolerance accepted: difference > 0 && percentDiff <= freightCostsPercent → email finance
     *      Incorrect billing:  otherwise
     *  - Persist status & emailSent
     */
    private void decideInvoiceStatusAndNotify(Invoice invoice, ToleranceLimitsResponseDTO tolerance) {
        BigDecimal invTotal = (invoice.getTotals() != null) ? invoice.getTotals().getGrossAmount() : null;
        BigDecimal diff = invoice.getInvoiceDifference();

        BigDecimal percentDiff = null;
        if (invTotal != null && diff != null && invTotal.compareTo(BigDecimal.ZERO) != 0) {
            percentDiff = diff.abs()
                    .divide(invTotal, 6, RoundingMode.HALF_UP)
                    .multiply(HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal freightTolerancePct = (tolerance != null) ? tolerance.getFreightCostsPercent() : null;

        log.info("Invoice percent diff computed. invoiceNumber={}, percentDiff={}, freightTolerancePct={}",
                safe(invoice.getInvoiceNumber()), percentDiff, freightTolerancePct);

        // Case A: Accepted (exact match)
        if (diff != null && diff.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus("Accepted");
            sendFinanceEmailSafe(templateAccepted, invoice, "Invoice accepted (exact match)");
            invoice.setEmailStatus(true);
            return;
        }

        // Case B: Over-billing within tolerance
        boolean overBilling = (diff != null && diff.compareTo(BigDecimal.ZERO) > 0);
        boolean withinTolerance = (percentDiff != null && freightTolerancePct != null
                && percentDiff.compareTo(freightTolerancePct) <= 0);

        if (overBilling && withinTolerance) {
            invoice.setStatus("Tolerance accepted");
            sendFinanceEmailSafe(templateToleranceAccepted, invoice, "Invoice within freight tolerance");
            invoice.setEmailStatus(true);
            return;
        }

        // Case C: Incorrect billing
        invoice.setStatus("Incorrect billing");
        invoice.setEmailStatus(false);
        log.warn("Invoice marked Incorrect billing. invoiceNumber={}, difference={}, percentDiff={}",
                safe(invoice.getInvoiceNumber()), diff, percentDiff);
    }

    // ---------------------- Email helper ----------------------

    private void sendFinanceEmailSafe(String templateId, Invoice invoice, String reason) {
        try {
            log.info("Sending finance email. templateId={}, to={}, invoiceNumber={}, reason={}",
                    templateId, financeEmail, safe(invoice.getInvoiceNumber()), reason);

            // Build a minimal payload. Expand as needed.
            EmailPayload payload = EmailPayload.builder()
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .companyId(invoice.getCompanyId())
                    .carrier(invoice.getSeller() != null ? invoice.getSeller().getCompanyName() : null)
                    .orderTotal(invoice.getOrderTotal())
                    .invoiceTotal(invoice.getTotals() != null ? invoice.getTotals().getGrossAmount() : null)
                    .difference(invoice.getInvoiceDifference())
                    .status(invoice.getStatus())
                    .build();
            // needs to change and send properhtml payload
            emailService.sendEmail(templateId, financeEmail, "chnage me with payload");
            log.info("Finance email sent successfully. invoiceNumber={}", safe(invoice.getInvoiceNumber()));
        } catch (Exception ex) {
            // Do not fail the reconciliation on email errors; just log.
            log.error("Failed to send finance email. invoiceNumber={}, templateId={}, error={}",
                    safe(invoice.getInvoiceNumber()), templateId, ex.getMessage(), ex);
        }
    }

    // ---------------------- Validation & Utils ----------------------

    private void validateInvoiceBasics(Invoice invoice) throws Exception {
        if (invoice.getSeller() == null || !StringUtils.hasText(invoice.getSeller().getCompanyName())) {
            throw new Exception("Seller/carrier name is required on invoice");
        }
        if (!StringUtils.hasText(invoice.getCompanyId())) {
            throw new Exception("companyId is required on invoice");
        }
        if (invoice.getTotals() == null || invoice.getTotals().getGrossAmount() == null) {
            throw new Exception("Invoice totals.grossAmount is required");
        }
    }

    private String safe(String s) { return s == null ? "" : s; }

    private BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b);
    }

    // --------- Minimal contract placeholders (adjust to your existing services) ---------

    /**
     * Tolerance service contract placeholder. Ensure your real service returns at least freightCostsPercent.
     */
//    public interface ToleranceService {
//        ToleranceLimits getTolerance(String companyId);
//    }

    /**
     * Internal shipment service contract placeholder.
     * Must return freightCost & extraCost for (shipmentId, projectId).
     */
//    public interface ShipmentService {
//        ShipmentData getShipmentData(String shipmentId, ObjectId projectId);
//    }

    /**
     * Shipper projects service contract placeholder. Already implemented in your codebase.
     */
//    public interface ShipperProjectService {
//        ObjectId getProjectIdByCarrier(String userId, String carrierName);
//    }

    /**
     * Email service contract placeholder. Plug your actual implementation.
     */
//    public interface EmailService {
//        void send(String templateId, String to, EmailPayload payload);
//    }

    // --------- Minimal DTOs used by the orchestrator ---------

    @lombok.Data
    @lombok.Builder
    public static class ShipmentData {
        private BigDecimal freightCost;
        private BigDecimal extraCost;
        // Add more fields here when the real business logic is available
    }

    @lombok.Data
    @lombok.Builder
    public static class EmailPayload {
        private String invoiceNumber;
        private String companyId;
        private String carrier;
        private BigDecimal orderTotal;
        private BigDecimal invoiceTotal;
        private BigDecimal difference;
        private String status;
    }
}
