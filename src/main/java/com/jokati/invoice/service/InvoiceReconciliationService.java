
package com.jokati.invoice.service;

import com.jokati.invoice.dto.ToleranceLimitsResponseDTO;
import com.jokati.invoice.model.Invoice;
import com.jokati.invoice.model.Shipment;
import com.jokati.invoice.model.ShipmentSummary;
import com.jokati.invoice.repository.InvoiceRepository;
import com.jokati.invoice.service.ShipmentSummaryService.RowSummedTotal;
import com.jokati.invoice.service.ShipmentSummaryService.ShipmentTotalSummary;
import com.jokati.invoice.service.ShipmentSummaryService.SummaryInitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import com.jokati.invoice.service.ShipperProjectService;
import com.jokati.invoice.service.EmailService;

/**
 * End-to-end orchestration for invoice adjudication.
 *
 * Flow: 1) Identify Carrier & Company (companyId, seller.company_name) 2)
 * Resolve project (ShipperProjectsService.getProjectIdByCarrier) 3) Enrich each
 * shipment by calling ShipmentSummaryService.getSummaryByShipmentId -
 * orderTotal(shipment) = sum(countriesRowTotal.totalPrice) -
 * surcharge(shipment) = sum(countriesRowTotal.totalExtraCostsPrice) -
 * difference = orderTotal - net_amount_eur - status = Correct billing |
 * Incorrect billing - raw summary attached for UI (optional) 4) Roll-up invoice
 * totals (Σ orderTotal) - invoiceDifference = orderTotal(invoice) -
 * invoice_total (totals.grossAmount) - percentDifference = |invoiceDifference|
 * / invoice_total * 100 5) Get tolerance
 * (ToleranceService.getTolerance(companyId).freightCostsPercent) 6) Determine
 * invoice status & Notify via EmailService - Accepted / Tolerance accepted /
 * Incorrect billing 7) Persist enriched invoice document 8) Respond enriched
 * InvoiceResponse
 * 
 * @param <S>
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceReconciliationService {

	private final InvoiceRepository invoiceRepository;
	private final ShipperProjectService shipperProjectsService;
	private final ShipmentSummaryService shipmentSummaryService;
	private final ToleranceLimitsService toleranceService;
	private final EmailService emailService;

	private final String templateIdAccepted = "template_accepted";
	private final String templateIdToleranceAccepted = "template_tolerance_accepted";

	public Invoice reconcileAndPersist(Invoice invoice) throws Exception {
		final String companyId = safe(invoice.getCompanyId());
		final String carrierName = invoice.getSeller() != null ? safe(invoice.getSeller().getCompanyName()) : "";

		log.info("Reconciling invoice: companyId={}, carrierName={}, invoiceNumber={}, date={}", companyId, carrierName,
				safe(invoice.getInvoiceNumber()), invoice.getInvoiceDate());

		final String projectIdHex = shipperProjectsService.getProjectIdHexByCarrier(companyId, carrierName);
		final ObjectId projectId = toObjectId(projectIdHex);

		if (invoice.getShipments() != null) {
			for (Shipment s : invoice.getShipments()) {
				enrichShipmentUsingSummary(s, projectId);
			}
		}

		BigDecimal invoiceOrderTotal = sumOrZero(invoice.getShipments(), Shipment::getOrderTotal);
		BigDecimal invoiceTotal = (invoice.getTotals() != null && invoice.getTotals().getGrossAmount() != null)
				? invoice.getTotals().getGrossAmount().setScale(2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		BigDecimal invoiceDifference = invoiceOrderTotal.subtract(invoiceTotal).setScale(2, RoundingMode.HALF_UP);
		BigDecimal percentDifference = percent(invoiceDifference, invoiceTotal);

		ToleranceLimitsResponseDTO tol = toleranceService.getByCompanyId(companyId);
		BigDecimal allowedPercent = (tol != null && tol.getFreightCostsPercent() != null)
				? tol.getFreightCostsPercent().setScale(2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		String invoiceStatus;
		boolean emailSent = false;
		String financeEmail = "satender.gautam@drishnatechnologies.com";// need to chnage in future

		if (invoiceDifference.compareTo(BigDecimal.ZERO) == 0) {
			invoiceStatus = "Accepted";
			emailSent = sendSafe(templateIdAccepted, financeEmail, Map.of("companyId", companyId, "carrierName",
					carrierName, "projectId", projectIdHex, "invoiceNumber", safe(invoice.getInvoiceNumber())));
		} else if (invoiceDifference.compareTo(BigDecimal.ZERO) > 0
				&& percentDifference.compareTo(allowedPercent) <= 0) {
			invoiceStatus = "Tolerance accepted";
			emailSent = sendSafe(templateIdToleranceAccepted, financeEmail,
					Map.of("companyId", companyId, "carrierName", carrierName, "projectId", projectIdHex,
							"invoiceNumber", safe(invoice.getInvoiceNumber()), "percentDifference", percentDifference));
		} else {
			invoiceStatus = "Incorrect billing";
		}

		// Shipment summary snapshot for invoice
		ShipmentSummary snapshot = buildShipmentSummary(invoice);
		invoice.setShipmentSummary(snapshot);

		invoice.setOrderTotal(invoiceOrderTotal);
		invoice.setInvoiceDifference(invoiceDifference);
		invoice.setStatus(invoiceStatus);
		invoice.setEmailStatus(emailSent);

		Invoice saved = invoiceRepository.save(invoice);
		log.info("Invoice saved: id={}, status={}, emailSent={}, date={}", saved.getId(), saved.getStatus(),
				saved.getEmailStatus(), LocalDate.now());
		return saved;
	}

	private void enrichShipmentUsingSummary(Shipment shipment, ObjectId projectId) {
		final String shipmentId = safe(shipment.getShipmentId());
		SummaryInitResult summary = shipmentSummaryService.getSummaryByShipmentId(projectId, shipmentId);

		BigDecimal orderTotal = BigDecimal.ZERO;

		if (summary != null && summary.getShipmentTotalSummary() != null) {
			ShipmentTotalSummary totals = summary.getShipmentTotalSummary();
			Map<String, RowSummedTotal> byCountry = totals.getCountriesRowTotal();
			if (byCountry != null) {
				for (RowSummedTotal rt : byCountry.values()) {
					orderTotal = orderTotal.add(BigDecimal.valueOf(rt.getTotalPrice()));
				}
			}
		} else {
			log.warn("No summary for shipmentId={}, set orderTotal=0", shipmentId);
		}

		orderTotal = orderTotal.setScale(2, RoundingMode.HALF_UP);
		BigDecimal netAmount = shipment.getNetAmountEur() != null
				? shipment.getNetAmountEur().setScale(2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		BigDecimal difference = orderTotal.subtract(netAmount).setScale(2, RoundingMode.HALF_UP);

		shipment.setOrderTotal(orderTotal);
		shipment.setDifference(difference);
		shipment.setStatus(difference.compareTo(BigDecimal.ZERO) == 0 ? "Correct billing" : "Incorrect billing");
	}

	private ShipmentSummary buildShipmentSummary(Invoice invoice) {
		int totalShipments = invoice.getShipments() == null ? 0 : invoice.getShipments().size();
		BigDecimal totalWeight = sumOrZero(invoice.getShipments(), Shipment::getWeightKg);
		return ShipmentSummary.builder().totalShipments(totalShipments).totalWeightKg(totalWeight).build();
	}

	// --- Helpers & contracts ---

	private BigDecimal sumOrZero(Iterable<Shipment> items, java.util.function.Function<Shipment, BigDecimal> f) {
		BigDecimal sum = BigDecimal.ZERO;
		if (items != null) {
			for (Shipment s : items) {
				BigDecimal v = f.apply(s);
				if (v != null)
					sum = sum.add(v);
			}
		}
		return sum.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal percent(BigDecimal diff, BigDecimal base) {
		if (base == null || base.abs().compareTo(BigDecimal.ZERO) == 0)
			return BigDecimal.ZERO;
		return diff.abs().divide(base, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2,
				RoundingMode.HALF_UP);
	}

	private String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private ObjectId toObjectId(String hex) {
		try {
			return new ObjectId(hex);
		} catch (IllegalArgumentException e) {
			return new ObjectId("000000000000000000000000");
		}
	}

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
