package com.jokati.invoice.service;

import static com.jokati.invoice.constants.BillingStatusConstants.ACCEPTED;
import static com.jokati.invoice.constants.BillingStatusConstants.COLOR_ERROR;
import static com.jokati.invoice.constants.BillingStatusConstants.COLOR_SUCCESS;
import static com.jokati.invoice.constants.BillingStatusConstants.COLOR_WARNING;
import static com.jokati.invoice.constants.BillingStatusConstants.CORRECT_BILLING;
import static com.jokati.invoice.constants.BillingStatusConstants.INCORRECT_BILLING;
import static com.jokati.invoice.constants.BillingStatusConstants.TOLERANCE_ACCEPTED;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.ToleranceLimitsResponseDTO;
import com.jokati.invoice.model.Charges;
import com.jokati.invoice.model.Invoice;
import com.jokati.invoice.model.Shipment;
import com.jokati.invoice.model.ShipmentItemDocument;
import com.jokati.invoice.model.StatusInfo;
import com.jokati.invoice.repository.InvoiceRepository;
import com.jokati.invoice.service.ShipmentSummaryService.SummaryInitResult;
import com.jokati.invoice.util.TextSanitizer;

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
	 * applies tolerance, sets status, optionally notifies finance, persists and
	 * returns.
	 */
	@Transactional
	public Invoice reconcileAndPersist(Invoice invoice) {
		final String companyId = safe(invoice.getCompanyId());
		final String carrierName = safe(invoice.getCarrier());

		if (companyId.isBlank()) {
			throw new IllegalArgumentException("companyId must not be blank");
		}
		if (carrierName.isBlank()) {
			throw new IllegalArgumentException("carrierName (seller.companyName) must not be blank");
		}

		log.info("Reconciling invoice: companyId={}, carrierName={}, invoiceNumber={}, date={}", companyId, carrierName,
				safe(invoice.getInvoiceNumber()), invoice.getInvoiceDate());

		// Resolve shipper project id for this company + carrier
		final String projectIdHex;
		try {
			projectIdHex = shipperProjectsService.getProjectIdHexByCarrier(companyId, carrierName);
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"No shipper project found for companyId=" + companyId + ", carrierName=" + carrierName, e);
		}

		final ObjectId projectId = toObjectIdStrict(projectIdHex);

		// Enrich each shipment with summary totals & status + orderCharges
		if (invoice.getShipments() != null) {
			for (Shipment s : invoice.getShipments()) {
				enrichShipmentUsingSummary(s, projectId);
			}
		}

		// Compute invoice-level totals from shipments (orderTotal is per-shipment total
		// price chosen by our selection logic)
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
			invoiceStatus = StatusInfo.builder().label(ACCEPTED).color(COLOR_SUCCESS).build();
			emailSent = sendSafe(templateIdAccepted, financeEmail, Map.of("companyId", companyId, "carrierName",
					carrierName, "projectId", projectIdHex, "invoiceNumber", safe(invoice.getInvoiceNumber())));
		} else if (invoiceDifference.compareTo(BigDecimal.ZERO) > 0 && percentDifference.compareTo(BigDecimal.ZERO) != 0
				&& percentDifference.compareTo(allowedPercent) <= 0) {
			invoiceStatus = StatusInfo.builder().label(TOLERANCE_ACCEPTED).color(COLOR_WARNING).build();
			emailSent = sendSafe(templateIdToleranceAccepted, financeEmail,
					Map.of("companyId", companyId, "carrierName", carrierName, "projectId", projectIdHex,
							"invoiceNumber", safe(invoice.getInvoiceNumber()), "percentDifference", percentDifference));
		} else {
			invoiceStatus = StatusInfo.builder().label(INCORRECT_BILLING).color(COLOR_ERROR).build();
		}

		invoice.setSystemStatus(invoiceStatus);

		statusHistoryService.saveSystem(invoice.getId(), invoiceStatus.getLabel());

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

	/**
	 * Enrich shipment totals from summary; sets orderTotal, difference, status,
	 * orderSurchargeTotal and orderCharges.
	 *
	 * Rule: - If consolidated sum rows exist for this shipmentId => use ONLY sum
	 * rows for totals - else use normal rows
	 */
	private void enrichShipmentUsingSummary(Shipment shipment, ObjectId projectId) {
		final String shipmentIdRaw = safe(shipment.getShipmentId());
		if (shipmentIdRaw.isBlank()) {
			log.warn("Skipping enrichment: shipmentId is blank");
			setDefaultsForShipment(shipment);
			return;
		}

		final String shipmentId = TextSanitizer.normalizeId(shipmentIdRaw);

		SummaryInitResult summary = shipmentSummaryService.getSummaryByShipmentId(projectId, shipmentId);

		BigDecimal orderTotal = BigDecimal.ZERO;
		BigDecimal extraCostsTotal = BigDecimal.ZERO;

		if (summary != null && summary.getConsolidatedShipmentData() != null) {

			Map<String, List<ShipmentItemDocument>> byCountry = summary.getConsolidatedShipmentData();

			boolean hasSumRows = hasConsolidatedSumRows(byCountry, shipmentId);

			for (List<ShipmentItemDocument> rows : byCountry.values()) {
				if (rows == null || rows.isEmpty())
					continue;

				for (ShipmentItemDocument row : rows) {
					if (row == null)
						continue;

					String rowShipmentId = TextSanitizer.normalizeId(row.getShipmentId());
					if (!shipmentId.equals(rowShipmentId))
						continue;

					boolean isSumRow = Boolean.TRUE.equals(row.getIsConsolidatedSum());
					if (hasSumRows && !isSumRow)
						continue;
					if (!hasSumRows && isSumRow)
						continue;

					orderTotal = orderTotal.add(nz(row.getTotalPrice()));
					extraCostsTotal = extraCostsTotal.add(nz(row.getExtraCostsTotalPrice()));
				}
			}
		} else {
			log.warn("No summary or consolidatedShipmentData for shipmentId={}, set totals=0", shipmentId);
		}

		orderTotal = scale2(orderTotal);
		extraCostsTotal = scale2(extraCostsTotal);

		BigDecimal netAmount = shipment.getTotalPrice() != null ? scale2(shipment.getTotalPrice()) : BigDecimal.ZERO;
		BigDecimal difference = scale2(netAmount.subtract(orderTotal)); // existing direction

		shipment.setOrderTotal(orderTotal);
		shipment.setDifference(difference);

		shipment.setStatus(difference.compareTo(BigDecimal.ZERO) == 0
				? StatusInfo.builder().label(CORRECT_BILLING).color(COLOR_SUCCESS).build()
				: StatusInfo.builder().label(INCORRECT_BILLING).color(COLOR_ERROR).build());

		shipment.setOrderSurchargeTotal(extraCostsTotal);
		// Full breakdown orderCharges (persist-safe)
		shipment.setOrderCharges(buildOrderChargesFullBreakdown(summary, shipmentId, shipment));
	}

	// ---------------------------------------------------------------------
	// FULL BREAKDOWN: orderCharges
	// ---------------------------------------------------------------------

	/**
	 * Builds orderCharges with full breakdown.
	 *
	 * Priority: - If sum row exists -> take ONLY sum row extraCosts amounts (no
	 * recomputation) for extras - Else -> prefer numeric amounts from each base
	 * row's extraCosts, fallback to catalog+flags - Palettentausch uses FP logic -
	 * Fixtermin, EmailAvis, BookingInAvis -> custom1..3; other leftovers
	 * (Express10/12/8/etc.) -> custom4..5 (sorted)
	 */
	private Charges buildOrderChargesFullBreakdown(SummaryInitResult summary, String shipmentId, Shipment shipment) {

		// Currency must be NOT blank due to @NotBlank in Charges
		String currency = "EUR";
		if (shipment != null && shipment.getCharges() != null && shipment.getCharges().getCurrency() != null
				&& !shipment.getCharges().getCurrency().isBlank()) {
			currency = shipment.getCharges().getCurrency();
		}

		// Collect all rows for this shipmentId (across all countries)
		List<ShipmentItemDocument> allRows = collectShipmentRows(summary, shipmentId);
		List<ShipmentItemDocument> rowsForTotals = selectRowsForTotals(allRows);
		boolean hasSumRows = rowsForTotals.stream()
				.anyMatch(r -> r != null && Boolean.TRUE.equals(r.getIsConsolidatedSum()));

		// Core totals (freight, toll, diesel) always taken from the selected rows (sum
		// rows if present, else base rows)
		BigDecimal freightCostSystem = BigDecimal.ZERO;
		BigDecimal tollCharge = BigDecimal.ZERO;
		BigDecimal dieselFee = BigDecimal.ZERO;

		for (ShipmentItemDocument r : rowsForTotals) {
			if (r == null)
				continue;
			freightCostSystem = freightCostSystem.add(nz(r.getPrice()));
			tollCharge = tollCharge.add(nz(r.getToll()));
			dieselFee = dieselFee.add(nz(r.getDiesel()));
		}
		freightCostSystem = scale2(freightCostSystem);
		tollCharge = scale2(tollCharge);
		dieselFee = scale2(dieselFee);

		// Extra costs to populate into Charges
		BigDecimal expressNextDay = BigDecimal.ZERO;
		BigDecimal phoneAvis = BigDecimal.ZERO;
		BigDecimal insurance = BigDecimal.ZERO;
		BigDecimal dangerousGoods = BigDecimal.ZERO;
		BigDecimal securityFee = BigDecimal.ZERO;
		BigDecimal longGoods = BigDecimal.ZERO;
		BigDecimal palletExchange = BigDecimal.ZERO;
		BigDecimal liftingPlatformSurcharge = BigDecimal.ZERO;

		// Custom slots policy
		BigDecimal custom1 = BigDecimal.ZERO; // Fixtermin
		BigDecimal custom2 = BigDecimal.ZERO; // EmailAvis
		BigDecimal custom3 = BigDecimal.ZERO; // BookingInAvis
		BigDecimal custom4 = BigDecimal.ZERO; // leftovers (e.g., Express10/12/8)
		BigDecimal custom5 = BigDecimal.ZERO; // leftovers

		if (hasSumRows) {
			// ---------- SUM ROW PATH: take extras ONLY from the sum row's extraCosts map
			// ----------
			Map<String, BigDecimal> extrasFromSum = aggregateExtrasFromSumRows(rowsForTotals);

			// 1) Map dedicated fields
			expressNextDay = extrasFromSum.getOrDefault(normKey("ExpressNextDay"), BigDecimal.ZERO);
			phoneAvis = extrasFromSum.getOrDefault(normKey("PhoneAvis"), BigDecimal.ZERO);
			insurance = firstNonNull(extrasFromSum.get(normKey("Versicherung")),
					extrasFromSum.get(normKey("Insurance")));
			dangerousGoods = firstNonNull(extrasFromSum.get(normKey("Gefahrgutzuschlag")),
					extrasFromSum.get(normKey("DangerousGoodsSurcharge")));
			securityFee = firstNonNull(extrasFromSum.get(normKey("SecurityFee")),
					extrasFromSum.get(normKey("Security Fee")));
			longGoods = firstNonNull(extrasFromSum.get(normKey("Langgutzuschlag")),
					extrasFromSum.get(normKey("LongGoodsSurcharge")));
			palletExchange = firstNonNull(extrasFromSum.get(normKey("Palettentausch")),
					extrasFromSum.get(normKey("PalletExchange")));
			liftingPlatformSurcharge = firstNonNull(extrasFromSum.get(normKey("Hebebühnenzuschlag")),
					extrasFromSum.get(normKey("Hebebuehnenzuschlag")), extrasFromSum.get(normKey("TailLiftSurcharge")));

			// 2) Reserve custom1..3 for Fixtermin / EmailAvis / BookingInAvis
			BigDecimal fixtermin = extrasFromSum.getOrDefault(normKey("Fixtermin"), BigDecimal.ZERO);
			BigDecimal emailAvisVal = firstNonNull(extrasFromSum.get(normKey("EmailAvis")),
					extrasFromSum.get(normKey("E-Mail Avis")));
			BigDecimal bookingInAvis = firstNonNull(extrasFromSum.get(normKey("BookingInAvis")),
					extrasFromSum.get(normKey("Booking in Avis")));
			custom1 = scale2(fixtermin);
			custom2 = scale2(emailAvisVal);
			custom3 = scale2(bookingInAvis);

			// 3) Consume known keys before distributing leftovers
			java.util.Set<String> consumed = new java.util.HashSet<>(java.util.Arrays.asList(normKey("ExpressNextDay"),
					normKey("PhoneAvis"), normKey("Versicherung"), normKey("Insurance"), normKey("Gefahrgutzuschlag"),
					normKey("DangerousGoodsSurcharge"), normKey("SecurityFee"), normKey("Security Fee"),
					normKey("Langgutzuschlag"), normKey("LongGoodsSurcharge"), normKey("Palettentausch"),
					normKey("PalletExchange"), normKey("Hebebühnenzuschlag"), normKey("Hebebuehnenzuschlag"),
					normKey("TailLiftSurcharge"), normKey("Fixtermin"), normKey("EmailAvis"), normKey("E-Mail Avis"),
					normKey("BookingInAvis"), normKey("Booking in Avis"),
					// ignore these in leftovers
					normKey("Toll"), normKey("Diesel")));

			// 4) Leftovers → fill into custom4..5 (sorted, deterministic). Includes
			// Express10/12/8 when present.
			java.util.List<Map.Entry<String, BigDecimal>> leftovers = extrasFromSum.entrySet().stream()
					.filter(e -> e.getValue() != null && e.getValue().compareTo(BigDecimal.ZERO) != 0)
					.filter(e -> !consumed.contains(e.getKey())).sorted(java.util.Map.Entry.comparingByKey()).toList();

			BigDecimal[] c = distributeToCustomsFromIndex(leftovers, 3 /* start at custom4 */);
			custom4 = c[3];
			custom5 = c[4];

			if (leftovers.size() > 2) {
				log.warn(
						"Shipment {}: {} extras left after dedicated fields and fixed customs; {} placed in custom4..5; {} dropped. {}",
						shipmentId, leftovers.size(), Math.min(2, leftovers.size()), Math.max(0, leftovers.size() - 2),
						leftovers);
			} else if (!leftovers.isEmpty()) {
				log.info("Shipment {}: extras mapped to custom4..5: {}", shipmentId, leftovers);
			}

		} else {
			// ---------- NO SUM ROW PATH: prefer row.extraCosts numeric amounts; fallback
			// to catalog+flag ----------
			Map<String, Object> extraCostsCatalog = (summary != null) ? summary.getFetchedShipperExtraCosts() : null;
			String country = resolveCountry(allRows);

			// Express family
			expressNextDay = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getExpressNextDay, extraCostsCatalog,
					country, "ExpressNextDay", "Express Next Day");
			BigDecimal express12 = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getExpress12, extraCostsCatalog,
					country, "Express12", "Express 12:00 Uhr");
			BigDecimal express10 = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getExpress10, extraCostsCatalog,
					country, "Express10", "Express 10:00 Uhr");
			BigDecimal express8 = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getExpress8, extraCostsCatalog,
					country, "Express8", "Express 08:00 Uhr");

			// Other modeled extras
			phoneAvis = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getPhoneAvis, extraCostsCatalog, country,
					"PhoneAvis", "Telefonisches Avis");
			insurance = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getInsurance, extraCostsCatalog, country,
					"Versicherung", "Insurance");
			dangerousGoods = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getDangerousGoodsSurcharge,
					extraCostsCatalog, country, "Gefahrgutzuschlag", "DangerousGoodsSurcharge");
			securityFee = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getSecurityFee, extraCostsCatalog,
					country, "SecurityFee", "Security Fee");
			longGoods = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getLongGoodsSurcharge, extraCostsCatalog,
					country, "LongGoodsSurcharge", "Langgutzuschlag");

			// Palettentausch: special FP logic
			palletExchange = computePalletExchange(extraCostsCatalog, country, allRows);

			// Tail lift
			liftingPlatformSurcharge = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getTailLiftSurcharge,
					extraCostsCatalog, country, "Hebebühnenzuschlag", "TailLiftSurcharge");

			// Reserve custom1..3 for Fixtermin / EmailAvis / BookingInAvis
			BigDecimal fixtermin = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getFixtermin, extraCostsCatalog,
					country, "Fixtermin");
			BigDecimal emailAvisVal = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getEmailAvis,
					extraCostsCatalog, country, "EmailAvis", "E-Mail Avis");
			BigDecimal bookingInAvis = sumByExtraCostsOrFlag(allRows, ShipmentItemDocument::getBookingInAvis,
					extraCostsCatalog, country, "BookingInAvis", "Booking in Avis");
			custom1 = scale2(fixtermin);
			custom2 = scale2(emailAvisVal);
			custom3 = scale2(bookingInAvis);

			// Put express12/express10/express8 into custom4..5 deterministically
			java.util.List<Map.Entry<String, BigDecimal>> leftovers = new java.util.ArrayList<>();
			if (express12.compareTo(BigDecimal.ZERO) != 0)
				leftovers.add(Map.entry(normKey("Express12"), express12));
			if (express10.compareTo(BigDecimal.ZERO) != 0)
				leftovers.add(Map.entry(normKey("Express10"), express10));
			if (express8.compareTo(BigDecimal.ZERO) != 0)
				leftovers.add(Map.entry(normKey("Express8"), express8));
			leftovers.sort(java.util.Map.Entry.comparingByKey());

			BigDecimal[] mergedCustoms = new BigDecimal[] { custom1, custom2, custom3, custom4, custom5 };
			BigDecimal[] filled = distributeSpecificIntoRemainingCustoms(mergedCustoms, leftovers,
					3 /* start at custom4 */);
			custom1 = filled[0];
			custom2 = filled[1];
			custom3 = filled[2];
			custom4 = filled[3];
			custom5 = filled[4];

			if (leftovers.size() > 2) {
				log.warn("Shipment {}: express extras exceed custom slots; {} placed in custom4..5, {} dropped. {}",
						shipmentId, Math.min(2, leftovers.size()), Math.max(0, leftovers.size() - 2), leftovers);
			}
		}

		return Charges.builder().freightCostSystem(scale2(freightCostSystem)).dieselFee(scale2(dieselFee))
				.tollCharge(scale2(tollCharge)).expressNextDay(scale2(expressNextDay))
				.palletExchange(scale2(palletExchange)).phoneAvis(scale2(phoneAvis))
				.liftingPlatformSurcharge(scale2(liftingPlatformSurcharge)).custom1(scale2(custom1))
				.custom2(scale2(custom2)).custom3(scale2(custom3)).custom4(scale2(custom4)).custom5(scale2(custom5))
				.insurance(scale2(insurance)).dangerousGoodsSurcharge(scale2(dangerousGoods))
				.securityFee(scale2(securityFee)).longGoodsSurcharge(scale2(longGoods)).currency(currency).build();
	}

	// ---------------------------------------------------------------------
	// Extra cost helpers (catalog + calculation)
	// ---------------------------------------------------------------------

	private static final class TermSpec {
		final String term;
		final BigDecimal value;
		final String unit;

		TermSpec(String term, BigDecimal value, String unit) {
			this.term = term;
			this.value = value == null ? BigDecimal.ZERO : value;
			this.unit = (unit == null || unit.isBlank()) ? "€" : unit.trim();
		}
	}

	@SuppressWarnings("unchecked")
	private TermSpec findTermSpec(Map<String, Object> extraCostsCatalog, String country, String term) {
		if (extraCostsCatalog == null || term == null)
			return null;

		TermSpec spec = findTermSpecInCountry(extraCostsCatalog, country, term);
		if (spec != null)
			return spec;

		return findTermSpecInCountry(extraCostsCatalog, "INT", term);
	}

	@SuppressWarnings("unchecked")
	private TermSpec findTermSpecInCountry(Map<String, Object> extraCostsCatalog, String country, String term) {
		Object nodeObj = extraCostsCatalog.get(country);
		if (!(nodeObj instanceof Map<?, ?>))
			return null;

		Map<String, Object> node = (Map<String, Object>) nodeObj;

		Object baseObj = node.get("Base");
		if (!(baseObj instanceof List<?> baseList))
			return null;

		for (Object itemObj : baseList) {
			if (!(itemObj instanceof Map<?, ?>))
				continue;
			Map<String, Object> item = (Map<String, Object>) itemObj;

			String t = String.valueOf(item.getOrDefault("Term", "")).trim();
			if (!term.equals(t))
				continue;

			String unit = String.valueOf(item.getOrDefault("Unit", "€")).trim();
			BigDecimal value = toBigDecimal(item.get("Value"));
			return new TermSpec(term, value, unit);
		}

		return null;
	}

	private BigDecimal toBigDecimal(Object v) {
		if (v == null)
			return BigDecimal.ZERO;
		if (v instanceof BigDecimal bd)
			return bd;
		if (v instanceof Number n)
			return BigDecimal.valueOf(n.doubleValue());
		try {
			return new BigDecimal(String.valueOf(v).trim());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	/**
	 * Computes extra amount for a row: - Unit "%" => percent of row net (row.price)
	 * - Unit "€" => fixed euro amount
	 */
	private BigDecimal computeExtraAmountForRow(TermSpec spec, BigDecimal rowNet) {
		if (spec == null)
			return BigDecimal.ZERO;
		if (rowNet == null)
			rowNet = BigDecimal.ZERO;

		if ("%".equals(spec.unit)) {
			return rowNet.multiply(spec.value).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
		}
		return spec.value;
	}

	private BigDecimal sumFlagExtra(List<ShipmentItemDocument> rows,
			java.util.function.Function<ShipmentItemDocument, Boolean> flagGetter,
			Map<String, Object> extraCostsCatalog, String country, String term) {
		if (rows == null || rows.isEmpty())
			return BigDecimal.ZERO;

		TermSpec spec = findTermSpec(extraCostsCatalog, country, term);
		if (spec == null)
			return BigDecimal.ZERO;

		BigDecimal sum = BigDecimal.ZERO;
		for (ShipmentItemDocument r : rows) {
			if (r == null)
				continue;
			if (!Boolean.TRUE.equals(flagGetter.apply(r)))
				continue;

			BigDecimal rowNet = nz(r.getPrice());
			sum = sum.add(computeExtraAmountForRow(spec, rowNet));
		}
		return scale2(sum);
	}

	/**
	 * Prefer per-row extraCosts numeric amount; otherwise compute via catalog+flag.
	 * Only used in "no sum row" path, iterates base rows.
	 */
	private BigDecimal sumByExtraCostsOrFlag(List<ShipmentItemDocument> rows,
			java.util.function.Function<ShipmentItemDocument, Boolean> flagGetter,
			Map<String, Object> extraCostsCatalog, String country, String... aliases) {
		if (rows == null || rows.isEmpty())
			return BigDecimal.ZERO;

		// normalized alias keys for quick lookup
		java.util.Set<String> aliasNorms = new java.util.HashSet<>();
		for (String a : aliases)
			aliasNorms.add(normKey(a));

		BigDecimal sum = BigDecimal.ZERO;

		for (ShipmentItemDocument r : rows) {
			if (r == null || Boolean.TRUE.equals(r.getIsConsolidatedSum()))
				continue; // base rows only

			// 1) Prefer explicit numeric values in the row's extraCosts
			Object ecObj = r.getExtraCosts();
			if (ecObj instanceof Map<?, ?> ec) {
				for (Map.Entry<?, ?> e : ((Map<?, ?>) ec).entrySet()) {
					String k = normKey(String.valueOf(e.getKey()));
					if (!aliasNorms.contains(k))
						continue;
					BigDecimal v = toBigDecimal(e.getValue());
					if (v != null && v.compareTo(BigDecimal.ZERO) != 0) {
						sum = sum.add(scale2(v));
					}
				}
			}

			// 2) If still nothing for this term and the row flag is set, compute via
			// catalog term (use the FIRST alias as catalog term)
			if (Boolean.TRUE.equals(flagGetter.apply(r))) {
				TermSpec spec = findTermSpec(extraCostsCatalog, country, aliases.length > 0 ? aliases[0] : null);
				sum = sum.add(scale2(computeExtraAmountForRow(spec, nz(r.getPrice()))));
			}
		}

		return scale2(sum);
	}

	/**
	 * Palettentausch: - Unit "%" => percent of net (row.price) - Unit "€" => fixed
	 * € per pallet
	 *
	 * Rule: - If consolidated sum row exists and hasFP=true => perPallet *
	 * fpPalletCount - else sum per FP row: perPallet * palletCount where
	 * packagingType=="FP"
	 */
	private BigDecimal computePalletExchange(Map<String, Object> extraCostsCatalog, String country,
			List<ShipmentItemDocument> allRows) {
		if (allRows == null || allRows.isEmpty())
			return BigDecimal.ZERO;

		TermSpec spec = findTermSpec(extraCostsCatalog, country, "Palettentausch");
		if (spec == null)
			return BigDecimal.ZERO;

		// Prefer sum row if present (it contains fpPalletCount / hasFP)
		ShipmentItemDocument sumRow = allRows.stream()
				.filter(r -> r != null && Boolean.TRUE.equals(r.getIsConsolidatedSum())).findFirst().orElse(null);

		if (sumRow != null && Boolean.TRUE.equals(sumRow.getHasFP())) {
			int fpPalletCount = sumRow.getFpPalletCount() == null ? 0 : sumRow.getFpPalletCount();
			BigDecimal perPallet = computeExtraAmountForRow(spec, nz(sumRow.getPrice()));
			return scale2(perPallet.multiply(BigDecimal.valueOf(fpPalletCount)));
		}

		// Otherwise compute from FP base rows
		BigDecimal total = BigDecimal.ZERO;
		for (ShipmentItemDocument r : allRows) {
			if (r == null)
				continue;
			if (Boolean.TRUE.equals(r.getIsConsolidatedSum()))
				continue;

			String pt = r.getPackagingType() == null ? "" : r.getPackagingType().trim();
			if (!"FP".equalsIgnoreCase(pt))
				continue;

			int palletCount = r.getPalletCount() == null ? 0 : r.getPalletCount();
			if (palletCount <= 0)
				continue;

			BigDecimal perPallet = computeExtraAmountForRow(spec, nz(r.getPrice()));
			total = total.add(perPallet.multiply(BigDecimal.valueOf(palletCount)));
		}

		return scale2(total);
	}

	// ---------------------------------------------------------------------
	// Row selection helpers (double-count safe)
	// ---------------------------------------------------------------------

	private List<ShipmentItemDocument> collectShipmentRows(SummaryInitResult summary, String shipmentId) {
		if (summary == null || summary.getConsolidatedShipmentData() == null)
			return List.of();

		Map<String, List<ShipmentItemDocument>> byCountry = summary.getConsolidatedShipmentData();
		java.util.ArrayList<ShipmentItemDocument> out = new java.util.ArrayList<>();

		for (List<ShipmentItemDocument> rows : byCountry.values()) {
			if (rows == null)
				continue;
			for (ShipmentItemDocument r : rows) {
				if (r == null)
					continue;
				String sid = TextSanitizer.normalizeId(r.getShipmentId());
				if (shipmentId.equals(sid))
					out.add(r);
			}
		}
		return out;
	}

	private List<ShipmentItemDocument> selectRowsForTotals(List<ShipmentItemDocument> allRows) {
		if (allRows == null || allRows.isEmpty())
			return List.of();

		boolean hasSum = allRows.stream().anyMatch(r -> r != null && Boolean.TRUE.equals(r.getIsConsolidatedSum()));
		if (hasSum) {
			return allRows.stream().filter(r -> r != null && Boolean.TRUE.equals(r.getIsConsolidatedSum())).toList();
		}
		return allRows.stream().filter(r -> r != null && !Boolean.TRUE.equals(r.getIsConsolidatedSum())).toList();
	}

	private String resolveCountry(List<ShipmentItemDocument> rows) {
		if (rows == null)
			return "INT";
		for (ShipmentItemDocument r : rows) {
			if (r == null)
				continue;
			String c = r.getCountry() == null ? "" : r.getCountry().trim();
			if (!c.isBlank())
				return c;
		}
		return "INT";
	}

	private boolean hasConsolidatedSumRows(Map<String, List<ShipmentItemDocument>> byCountry, String shipmentId) {
		if (byCountry == null || byCountry.isEmpty())
			return false;

		for (List<ShipmentItemDocument> rows : byCountry.values()) {
			if (rows == null)
				continue;
			for (ShipmentItemDocument r : rows) {
				if (r == null)
					continue;
				String sid = TextSanitizer.normalizeId(r.getShipmentId());
				if (shipmentId.equals(sid) && Boolean.TRUE.equals(r.getIsConsolidatedSum())) {
					return true;
				}
			}
		}
		return false;
	}

	// ---------------------------------------------------------------------
	// Defaults + generic helpers
	// ---------------------------------------------------------------------

	private void setDefaultsForShipment(Shipment s) {
		s.setOrderTotal(BigDecimal.ZERO);
		s.setDifference(BigDecimal.ZERO);
		s.setStatus(StatusInfo.builder().label("Incorrect billing").color("#dc3545").build());
		// Persist-safe defaults if shipment gets saved
		s.setOrderSurchargeTotal(BigDecimal.ZERO);
		if (s.getOrderCharges() == null) {
			String currency = (s.getCharges() != null && s.getCharges().getCurrency() != null
					&& !s.getCharges().getCurrency().isBlank()) ? s.getCharges().getCurrency() : "EUR";
			s.setOrderCharges(zeroCharges(currency));
		}
	}

	private Charges zeroCharges(String currency) {
		String cur = (currency == null || currency.isBlank()) ? "EUR" : currency;
		return Charges.builder().freightCostSystem(BigDecimal.ZERO).dieselFee(BigDecimal.ZERO)
				.tollCharge(BigDecimal.ZERO).expressNextDay(BigDecimal.ZERO).palletExchange(BigDecimal.ZERO)
				.phoneAvis(BigDecimal.ZERO).liftingPlatformSurcharge(BigDecimal.ZERO).custom1(BigDecimal.ZERO)
				.custom2(BigDecimal.ZERO).custom3(BigDecimal.ZERO).custom4(BigDecimal.ZERO).custom5(BigDecimal.ZERO)
				.insurance(BigDecimal.ZERO).dangerousGoodsSurcharge(BigDecimal.ZERO).securityFee(BigDecimal.ZERO)
				.longGoodsSurcharge(BigDecimal.ZERO).currency(cur).build();
	}

	private BigDecimal sumOrZero(Iterable<Shipment> items, java.util.function.Function<Shipment, BigDecimal> f) {
		BigDecimal sum = BigDecimal.ZERO;
		if (items != null) {
			for (Shipment s : items) {
				BigDecimal v = f.apply(s);
				if (v != null)
					sum = sum.add(v);
			}
		}
		return scale2(sum);
	}

	private BigDecimal percent(BigDecimal diff, BigDecimal base) {
		if (base == null || base.abs().compareTo(BigDecimal.ZERO) == 0)
			return BigDecimal.ZERO;
		return diff.abs().divide(base, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2,
				RoundingMode.HALF_UP);
	}

	private BigDecimal scale2(BigDecimal v) {
		return (v == null) ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
	}

	private String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private BigDecimal nz(Double v) {
		return v == null ? BigDecimal.ZERO : BigDecimal.valueOf(v);
	}

	private BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}

	private ObjectId toObjectIdStrict(String hex) {
		try {
			return new ObjectId(hex);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid ObjectId hex: " + hex, e);
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

	// ---------------------------------------------------------------------
	// NEW helpers for extraCosts mapping & normalization
	// ---------------------------------------------------------------------

	@SafeVarargs
	private final BigDecimal firstNonNull(BigDecimal... values) {
		for (BigDecimal v : values) {
			if (v != null)
				return v;
		}
		return BigDecimal.ZERO;
	}

	private String normKey(String key) {
		if (key == null)
			return "";
		String s = java.text.Normalizer.normalize(key, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", ""); // strip
																												// diacritics
		s = s.replaceAll("[^A-Za-z0-9]", ""); // keep only letters/digits
		return s.toUpperCase(java.util.Locale.ROOT);
	}

	/**
	 * When consolidated sum rows are present, aggregate their extraCosts maps into
	 * a normalized map. Keys are normalized via normKey(), values summed as
	 * BigDecimal.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, BigDecimal> aggregateExtrasFromSumRows(List<ShipmentItemDocument> sumRows) {
		Map<String, BigDecimal> out = new java.util.HashMap<>();
		if (sumRows == null)
			return out;

		for (ShipmentItemDocument r : sumRows) {
			if (r == null || !Boolean.TRUE.equals(r.getIsConsolidatedSum()))
				continue;

			Object ecObj = r.getExtraCosts();
			if (!(ecObj instanceof Map<?, ?> ec))
				continue;

			for (Map.Entry<?, ?> e : ec.entrySet()) {
				String k = normKey(String.valueOf(e.getKey()));
				BigDecimal v = toBigDecimal(e.getValue());
				out.merge(k, scale2(v), BigDecimal::add);
			}
		}
		return out;
	}

	/**
	 * Distribute leftovers into custom slots starting at a given index (0-based).
	 */
	private BigDecimal[] distributeToCustomsFromIndex(java.util.List<Map.Entry<String, BigDecimal>> leftovers,
			int startAt) {
		BigDecimal[] custom = new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO };
		int i = Math.max(0, startAt);
		for (Map.Entry<String, BigDecimal> e : leftovers) {
			if (i > 4)
				break;
			custom[i++] = scale2(e.getValue());
		}
		return custom;
	}

	/**
	 * Fill custom4..5 (starting at 'startAt') with specific entries, preserving
	 * already-set custom1..3.
	 */
	private BigDecimal[] distributeSpecificIntoRemainingCustoms(BigDecimal[] current,
			java.util.List<Map.Entry<String, BigDecimal>> items, int startAt) {
		BigDecimal[] out = java.util.Arrays.copyOf(current, current.length);
		int i = Math.max(0, startAt);
		for (Map.Entry<String, BigDecimal> e : items) {
			if (i > 4)
				break;
			out[i++] = scale2(e.getValue());
		}
		return out;
	}
}