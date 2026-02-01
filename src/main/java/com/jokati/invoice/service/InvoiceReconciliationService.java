
package com.jokati.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.ToleranceLimitsResponseDTO;
import com.jokati.invoice.model.Invoice;
import com.jokati.invoice.model.Shipment;
import com.jokati.invoice.model.ShipmentSummary;
import com.jokati.invoice.model.StatusInfo;
import com.jokati.invoice.repository.InvoiceRepository;
import com.jokati.invoice.service.ShipmentSummaryService.RowSummedTotal;
import com.jokati.invoice.service.ShipmentSummaryService.ShipmentTotalSummary;
import com.jokati.invoice.service.ShipmentSummaryService.SummaryInitResult;
import static com.jokati.invoice.constants.BillingStatusConstants.*;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceReconciliationService {

    private final InvoiceRepository invoiceRepository;
    private final ShipperProjectService shipperProjectsService;
    private final ShipmentSummaryService shipmentSummaryService;
    private final ToleranceLimitsService toleranceService;
    private final EmailService emailService;
    private final InvoiceStatusHistoryService statusHistoryService;


    /** Externalized (can be moved to @ConfigurationProperties). */
    @Value("${notification.templates.accepted:template_accepted}")
    private String templateIdAccepted;

    @Value("${notification.templates.toleranceAccepted:template_tolerance_accepted}")
    private String templateIdToleranceAccepted;

    @Value("${notification.financeEmail:}")
    private String financeEmail; // Keep empty-safe; if blank, email is skipped

    /**
     * Enriches shipments, computes invoice totals/difference vs. gross amount,
     * applies tolerance, sets status, optionally notifies finance, persists and returns.
     */
    @Transactional
    public Invoice reconcileAndPersist(Invoice invoice) {
        final String companyId   = safe(invoice.getCompanyId());
        final String carrierName = safe(invoice.getCarrier());

        if (companyId.isBlank()) {
            throw new IllegalArgumentException("companyId must not be blank");
        }
        if (carrierName.isBlank()) {
            throw new IllegalArgumentException("carrierName (seller.companyName) must not be blank");
        }

        log.info("Reconciling invoice: companyId={}, carrierName={}, invoiceNumber={}, date={}",
                companyId, carrierName, safe(invoice.getInvoiceNumber()), invoice.getInvoiceDate());

        // Resolve shipper project id for this company + carrier
        final String projectIdHex;
        try {
            projectIdHex = shipperProjectsService.getProjectIdHexByCarrier(companyId, carrierName);  // companyId == userId and carrierName = name in document
        } catch (Exception e) {
            // Convert checked to runtime for global handler
            throw new IllegalArgumentException("No shipper project found for companyId=" + companyId +
                    ", carrierName=" + carrierName, e);
        }

        final ObjectId projectId = toObjectIdStrict(projectIdHex);

        // Enrich each shipment with summary totals & status
        if (invoice.getShipments() != null) {
            for (Shipment s : invoice.getShipments()) {
                enrichShipmentUsingSummary(s, projectId);
            }
        }

        // Compute invoice-level totals
        BigDecimal invoiceOrderTotal = sumOrZero(invoice.getShipments(), Shipment::getOrderTotal);
        BigDecimal invoiceGrossTotal = (invoice.getTotals() != null && invoice.getTotals().getGrossAmount() != null)
                ? scale2(invoice.getTotals().getGrossAmount())
                : BigDecimal.ZERO;

        BigDecimal invoiceDifference = scale2(invoiceGrossTotal.subtract(invoiceOrderTotal));
        BigDecimal percentDifference = percent(invoiceDifference, invoiceOrderTotal);

        // Get tolerance
        ToleranceLimitsResponseDTO tol = toleranceService.getByCompanyId(companyId);
        BigDecimal allowedPercent = (tol != null && tol.getFreightCostsPercent() != null)
                ? scale2(tol.getFreightCostsPercent())
                : BigDecimal.ZERO;

        // Determine status & notify
        StatusInfo invoiceStatus;
        boolean emailSent = false;

        if (invoiceDifference.compareTo(BigDecimal.ZERO) == 0) {
            invoiceStatus = StatusInfo.builder()
            		.label(ACCEPTED)
                    .color(COLOR_SUCCESS)
                    .build();
            emailSent = sendSafe(templateIdAccepted, financeEmail, Map.of(
                    "companyId", companyId,
                    "carrierName", carrierName,
                    "projectId", projectIdHex,
                    "invoiceNumber", safe(invoice.getInvoiceNumber())
            ));
        } else if (invoiceDifference.compareTo(BigDecimal.ZERO) > 0
                && percentDifference.compareTo(allowedPercent) <= 0) {   //this logic needs to be changed
            invoiceStatus = StatusInfo.builder()
            		.label(TOLERANCE_ACCEPTED)
                    .color(COLOR_WARNING)
                    .build();
            emailSent = sendSafe(templateIdToleranceAccepted, financeEmail, Map.of(
                    "companyId", companyId,
                    "carrierName", carrierName,
                    "projectId", projectIdHex,
                    "invoiceNumber", safe(invoice.getInvoiceNumber()),
                    "percentDifference", percentDifference
            ));
        } else {
        	invoiceStatus = StatusInfo.builder()
        			.label(INCORRECT_BILLING)
                    .color(COLOR_ERROR)
                    .build();
        }
        
        invoice.setSystemStatus(invoiceStatus);

        statusHistoryService.saveSystem(
            invoice.getId(),
            invoiceStatus.getLabel()
        );

        // Persist enriched invoice fields
        invoice.setOrderTotal(invoiceOrderTotal);
        invoice.setInvoiceDifference(invoiceDifference);
        invoice.setSystemStatus(invoiceStatus);
        invoice.setEmailStatus(emailSent);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice saved: id={}, status={}, emailSent={}, date={}", saved.getId(), saved.getSystemStatus(),
                saved.getEmailStatus(), LocalDate.now());
        return saved;
    }

    /** Enrich shipment totals from summary; sets orderTotal, difference, status. */
    private void enrichShipmentUsingSummary(Shipment shipment, ObjectId projectId) {
        final String shipmentId = safe(shipment.getShipmentId());
        if (shipmentId.isBlank()) {
            log.warn("Skipping enrichment: shipmentId is blank");
            setDefaultsForShipment(shipment);
            return;
        }
        
        SummaryInitResult summary = shipmentSummaryService.getSummaryByShipmentId(projectId, shipmentId);

        BigDecimal orderTotal      = BigDecimal.ZERO; // Σ totalPrice per-country
        BigDecimal extraCostsTotal = BigDecimal.ZERO; // Σ totalExtraCostsPrice per-country

        if (summary != null && summary.getShipmentTotalSummary() != null) {
            ShipmentTotalSummary totals = summary.getShipmentTotalSummary();
            Map<String, RowSummedTotal> byCountry = totals.getCountriesRowTotal();

            if (byCountry != null) {
                for (RowSummedTotal rt : byCountry.values()) {
                    if (rt != null) {
                        // FIX #1: do not compare primitive double to null; just sum
                        orderTotal      = orderTotal.add(BigDecimal.valueOf(rt.getTotalPrice()));
                        // FIX #2: totalExtraCostsPrice is primitive; never null; sum directly
                        extraCostsTotal = extraCostsTotal.add(BigDecimal.valueOf(rt.getTotalExtraCostsPrice()));
                    }
                }
            }
        } else {
            log.warn("No summary for shipmentId={}, set orderTotal=0", shipmentId);
        }

        orderTotal = scale2(orderTotal);
        BigDecimal netAmount  = shipment.getTotalPrice() != null ? scale2(shipment.getTotalPrice()) : BigDecimal.ZERO;
        BigDecimal difference = scale2(orderTotal.subtract(netAmount));

        shipment.setOrderTotal(orderTotal);
        shipment.setDifference(difference); 
        shipment.setStatus(
        	    difference.compareTo(BigDecimal.ZERO) == 0
        	        ? StatusInfo.builder().label(CORRECT_BILLING).color(COLOR_SUCCESS).build()
        	        : StatusInfo.builder().label(INCORRECT_BILLING).color(COLOR_ERROR).build()
        	);


        // (Optional) if your Shipment model has a surcharge field, set it here:
        // shipment.setSurchargeTotal(scale2(extraCostsTotal));
    }

    private void setDefaultsForShipment(Shipment s) {
        s.setOrderTotal(BigDecimal.ZERO);
        s.setDifference(BigDecimal.ZERO);
        s.setStatus(StatusInfo.builder().label("Incorrect billing").color("#dc3545").build());
    }

    // ---------------- Helpers ----------------

    private BigDecimal sumOrZero(Iterable<Shipment> items, java.util.function.Function<Shipment, BigDecimal> f) {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (Shipment s : items) {
                BigDecimal v = f.apply(s);
                if (v != null) sum = sum.add(v);
            }
        }
        return scale2(sum);
    }

    private BigDecimal percent(BigDecimal diff, BigDecimal base) {
        if (base == null || base.abs().compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return diff.abs()
                .divide(base, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale2(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /** Strict ObjectId parser: invalid hex -> BAD_REQUEST via IllegalArgumentException. */
    private ObjectId toObjectIdStrict(String hex) {
        try {
            return new ObjectId(hex);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ObjectId hex: " + hex, e);
        }
    }

    /**
     * Email send is best-effort (no exception propagation).
     * If recipient is blank, skip with a warning.
     */
    private boolean sendSafe(String templateId, String to, Map<String, Object> model) {
        try {
            if (to == null || to.isBlank()) {
                log.warn("Email skipped (recipient empty) templateId={}", templateId);
                return false;
            }
             emailService.send(templateId, to, model);
            return true;
         } catch (Exception ex) {
            log.error("Email failed: {}", ex.getMessage(), ex);
            return false;
        }
    }
}
