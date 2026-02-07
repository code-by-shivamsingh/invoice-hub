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
            projectIdHex = shipperProjectsService.getProjectIdHexByCarrier(companyId, carrierName);
        } catch (Exception e) {
            throw new IllegalArgumentException("No shipper project found for companyId=" + companyId +
                    ", carrierName=" + carrierName, e);
        }

        final ObjectId projectId = toObjectIdStrict(projectIdHex);

        // Enrich each shipment with summary totals & status + orderCharges
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
                && percentDifference.compareTo(BigDecimal.ZERO) != 0
                && percentDifference.compareTo(allowedPercent) <= 0) {
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
        log.info("Invoice saved: id={}, status={}, emailSent={}, date={}",
                saved.getId(), saved.getSystemStatus(), saved.getEmailStatus(), LocalDate.now());
        return saved;
    }

    /**
     * Enrich shipment totals from summary; sets orderTotal, difference, status, orderSurchargeTotal and orderCharges.
     *
     * Production fix:
     * - If consolidated sum rows exist for this shipmentId => use ONLY sum rows for totals
     * - else use normal rows
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

        BigDecimal orderTotal      = BigDecimal.ZERO;
        BigDecimal extraCostsTotal = BigDecimal.ZERO;

        if (summary != null && summary.getConsolidatedShipmentData() != null) {

            Map<String, List<ShipmentItemDocument>> byCountry = summary.getConsolidatedShipmentData();

            boolean hasSumRows = hasConsolidatedSumRows(byCountry, shipmentId);

            for (List<ShipmentItemDocument> rows : byCountry.values()) {
                if (rows == null || rows.isEmpty()) continue;

                for (ShipmentItemDocument row : rows) {
                    if (row == null) continue;

                    String rowShipmentId = TextSanitizer.normalizeId(row.getShipmentId());
                    if (!shipmentId.equals(rowShipmentId)) continue;

                    boolean isSumRow = Boolean.TRUE.equals(row.getIsConsolidatedSum());
                    if (hasSumRows && !isSumRow) continue;
                    if (!hasSumRows && isSumRow) continue;

                    orderTotal      = orderTotal.add(nz(row.getTotalPrice()));
                    extraCostsTotal = extraCostsTotal.add(nz(row.getExtraCostsTotalPrice()));
                }
            }
        } else {
            log.warn("No summary or consolidatedShipmentData for shipmentId={}, set totals=0", shipmentId);
        }

        orderTotal      = scale2(orderTotal);
        extraCostsTotal = scale2(extraCostsTotal);

        BigDecimal netAmount  = shipment.getTotalPrice() != null ? scale2(shipment.getTotalPrice()) : BigDecimal.ZERO;
        BigDecimal difference = scale2(netAmount.subtract(orderTotal)); // keep your existing direction

        shipment.setOrderTotal(orderTotal);
        shipment.setDifference(difference);

        shipment.setStatus(
                difference.compareTo(BigDecimal.ZERO) == 0
                        ? StatusInfo.builder().label(CORRECT_BILLING).color(COLOR_SUCCESS).build()
                        : StatusInfo.builder().label(INCORRECT_BILLING).color(COLOR_ERROR).build()
        );

        shipment.setOrderSurchargeTotal(extraCostsTotal);

        // ✅ Full breakdown orderCharges (persist-safe)
        shipment.setOrderCharges(buildOrderChargesFullBreakdown(summary, shipmentId, shipment));
    }

    // ---------------------------------------------------------------------
    // FULL BREAKDOWN: orderCharges
    // ---------------------------------------------------------------------

    /**
     * Builds orderCharges with full breakdown for fields available in Charges model:
     * - freightCostSystem, dieselFee, tollCharge from summary row values
     * - expressNextDay, phoneAvis, insurance, dangerousGoodsSurcharge, securityFee, longGoodsSurcharge, custom1..5 from flags + extraCosts catalog
     * - palletExchange uses special pallet logic (FP pallets)
     * - liftingPlatformSurcharge kept 0 (as requested)
     *
     * Persist-safe: fills all @NotNull BigDecimal fields + currency @NotBlank.
     */
    private Charges buildOrderChargesFullBreakdown(SummaryInitResult summary, String shipmentId, Shipment shipment) {

        // Currency must be NOT blank due to @NotBlank in Charges
        String currency = "EUR";
        if (shipment != null && shipment.getCharges() != null
                && shipment.getCharges().getCurrency() != null
                && !shipment.getCharges().getCurrency().isBlank()) {
            currency = shipment.getCharges().getCurrency();
        }

        // Collect all rows for this shipmentId (across all countries)
        List<ShipmentItemDocument> allRows = collectShipmentRows(summary, shipmentId);
        List<ShipmentItemDocument> rowsForTotals = selectRowsForTotals(allRows);

        // These should match summary totals reliably
        BigDecimal freightCostSystem = BigDecimal.ZERO;
        BigDecimal tollCharge        = BigDecimal.ZERO;
        BigDecimal dieselFee         = BigDecimal.ZERO;

        for (ShipmentItemDocument r : rowsForTotals) {
            freightCostSystem = freightCostSystem.add(nz(r.getPrice()));
            tollCharge        = tollCharge.add(nz(r.getToll()));
            dieselFee         = dieselFee.add(nz(r.getDiesel()));
        }

        freightCostSystem = scale2(freightCostSystem);
        tollCharge        = scale2(tollCharge);
        dieselFee         = scale2(dieselFee);

        // Extra cost catalog from summary
        Map<String, Object> extraCostsCatalog = (summary != null) ? summary.getFetchedShipperExtraCosts() : null;
        String country = resolveCountry(allRows);

        // Compute per-flag extras by summing per-row (matches row-level TS/Java behavior)
        BigDecimal expressNextDay = sumFlagExtra(allRows, ShipmentItemDocument::getExpressNextDay,
                extraCostsCatalog, country, "Express Next Day");

        BigDecimal phoneAvis = sumFlagExtra(allRows, ShipmentItemDocument::getPhoneAvis,
                extraCostsCatalog, country, "Telefonisches Avis");

        BigDecimal insurance = sumFlagExtra(allRows, ShipmentItemDocument::getInsurance,
                extraCostsCatalog, country, "Versicherung");

        BigDecimal dangerousGoods = sumFlagExtra(allRows, ShipmentItemDocument::getDangerousGoodsSurcharge,
                extraCostsCatalog, country, "Gefahrgutzuschlag");

        BigDecimal securityFee = sumFlagExtra(allRows, ShipmentItemDocument::getSecurityFee,
                extraCostsCatalog, country, "Security Fee");

        BigDecimal longGoods = sumFlagExtra(allRows, ShipmentItemDocument::getLongGoodsSurcharge,
                extraCostsCatalog, country, "Langgutzuschlag");

        BigDecimal custom1 = sumFlagExtra(allRows, ShipmentItemDocument::getCustom1,
                extraCostsCatalog, country, "Eigene 1");

        BigDecimal custom2 = sumFlagExtra(allRows, ShipmentItemDocument::getCustom2,
                extraCostsCatalog, country, "Eigene 2");

        BigDecimal custom3 = sumFlagExtra(allRows, ShipmentItemDocument::getCustom3,
                extraCostsCatalog, country, "Eigene 3");

        BigDecimal custom4 = sumFlagExtra(allRows, ShipmentItemDocument::getCustom4,
                extraCostsCatalog, country, "Eigene 4");

        BigDecimal custom5 = sumFlagExtra(allRows, ShipmentItemDocument::getCustom5,
                extraCostsCatalog, country, "Eigene 5");

        // Palettentausch: special rule (FP pallets), best derived from sum row if exists else from base rows
        BigDecimal palletExchange = computePalletExchange(extraCostsCatalog, country, allRows);

        // As requested: leave Hebebühnenzuschlag for now
        BigDecimal liftingPlatformSurcharge = BigDecimal.ZERO;

        // Persist-safe Charges
        return Charges.builder()
                .freightCostSystem(scale2(freightCostSystem))
                .dieselFee(scale2(dieselFee))
                .tollCharge(scale2(tollCharge))
                .expressNextDay(scale2(expressNextDay))
                .palletExchange(scale2(palletExchange))
                .phoneAvis(scale2(phoneAvis))
                .liftingPlatformSurcharge(scale2(liftingPlatformSurcharge))
                .custom1(scale2(custom1))
                .custom2(scale2(custom2))
                .custom3(scale2(custom3))
                .custom4(scale2(custom4))
                .custom5(scale2(custom5))
                .insurance(scale2(insurance))
                .dangerousGoodsSurcharge(scale2(dangerousGoods))
                .securityFee(scale2(securityFee))
                .longGoodsSurcharge(scale2(longGoods))
                .currency(currency)
                .build();
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
        if (extraCostsCatalog == null || term == null) return null;

        TermSpec spec = findTermSpecInCountry(extraCostsCatalog, country, term);
        if (spec != null) return spec;

        return findTermSpecInCountry(extraCostsCatalog, "INT", term);
    }

    @SuppressWarnings("unchecked")
    private TermSpec findTermSpecInCountry(Map<String, Object> extraCostsCatalog, String country, String term) {
        Object nodeObj = extraCostsCatalog.get(country);
        if (!(nodeObj instanceof Map<?, ?>)) return null;

        Map<String, Object> node = (Map<String, Object>) nodeObj;

        Object baseObj = node.get("Base");
        if (!(baseObj instanceof List<?> baseList)) return null;

        for (Object itemObj : baseList) {
            if (!(itemObj instanceof Map<?, ?>)) continue;
            Map<String, Object> item = (Map<String, Object>) itemObj;

            String t = String.valueOf(item.getOrDefault("Term", "")).trim();
            if (!term.equals(t)) continue;

            String unit = String.valueOf(item.getOrDefault("Unit", "€")).trim();
            BigDecimal value = toBigDecimal(item.get("Value"));
            return new TermSpec(term, value, unit);
        }

        return null;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Computes extra amount for a row:
     * - Unit "%" => percent of row net (row.price)
     * - Unit "€" => fixed euro amount
     */
    private BigDecimal computeExtraAmountForRow(TermSpec spec, BigDecimal rowNet) {
        if (spec == null) return BigDecimal.ZERO;
        if (rowNet == null) rowNet = BigDecimal.ZERO;

        if ("%".equals(spec.unit)) {
            return rowNet.multiply(spec.value)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        }
        return spec.value;
    }

    private BigDecimal sumFlagExtra(List<ShipmentItemDocument> rows,
                                    java.util.function.Function<ShipmentItemDocument, Boolean> flagGetter,
                                    Map<String, Object> extraCostsCatalog,
                                    String country,
                                    String term) {
        if (rows == null || rows.isEmpty()) return BigDecimal.ZERO;

        TermSpec spec = findTermSpec(extraCostsCatalog, country, term);
        if (spec == null) return BigDecimal.ZERO;

        BigDecimal sum = BigDecimal.ZERO;
        for (ShipmentItemDocument r : rows) {
            if (r == null) continue;
            if (!Boolean.TRUE.equals(flagGetter.apply(r))) continue;

            BigDecimal rowNet = nz(r.getPrice());
            sum = sum.add(computeExtraAmountForRow(spec, rowNet));
        }
        return scale2(sum);
    }

    /**
     * Palettentausch:
     * - Unit "%" => percent of net (row.price)
     * - Unit "€" => fixed € per pallet
     *
     * Rule:
     * - If consolidated sum row exists and hasFP=true => perPallet * fpPalletCount
     * - else sum per FP row: perPallet * palletCount where packagingType=="FP"
     */
    private BigDecimal computePalletExchange(Map<String, Object> extraCostsCatalog, String country, List<ShipmentItemDocument> allRows) {
        if (allRows == null || allRows.isEmpty()) return BigDecimal.ZERO;

        TermSpec spec = findTermSpec(extraCostsCatalog, country, "Palettentausch");
        if (spec == null) return BigDecimal.ZERO;

        // Prefer sum row if present (it contains fpPalletCount / hasFP)
        ShipmentItemDocument sumRow = allRows.stream()
                .filter(r -> r != null && Boolean.TRUE.equals(r.getIsConsolidatedSum()))
                .findFirst()
                .orElse(null);

        if (sumRow != null && Boolean.TRUE.equals(sumRow.getHasFP())) {
            int fpPalletCount = sumRow.getFpPalletCount() == null ? 0 : sumRow.getFpPalletCount();

            BigDecimal perPallet = computeExtraAmountForRow(spec, nz(sumRow.getPrice()));
            return scale2(perPallet.multiply(BigDecimal.valueOf(fpPalletCount)));
        }

        // Otherwise compute from FP base rows
        BigDecimal total = BigDecimal.ZERO;
        for (ShipmentItemDocument r : allRows) {
            if (r == null) continue;
            if (Boolean.TRUE.equals(r.getIsConsolidatedSum())) continue;

            String pt = r.getPackagingType() == null ? "" : r.getPackagingType().trim();
            if (!"FP".equalsIgnoreCase(pt)) continue;

            int palletCount = r.getPalletCount() == null ? 0 : r.getPalletCount();
            if (palletCount <= 0) continue;

            BigDecimal perPallet = computeExtraAmountForRow(spec, nz(r.getPrice()));
            total = total.add(perPallet.multiply(BigDecimal.valueOf(palletCount)));
        }

        return scale2(total);
    }

    // ---------------------------------------------------------------------
    // Row selection helpers (double-count safe)
    // ---------------------------------------------------------------------

    private List<ShipmentItemDocument> collectShipmentRows(SummaryInitResult summary, String shipmentId) {
        if (summary == null || summary.getConsolidatedShipmentData() == null) return List.of();

        Map<String, List<ShipmentItemDocument>> byCountry = summary.getConsolidatedShipmentData();
        java.util.ArrayList<ShipmentItemDocument> out = new java.util.ArrayList<>();

        for (List<ShipmentItemDocument> rows : byCountry.values()) {
            if (rows == null) continue;
            for (ShipmentItemDocument r : rows) {
                if (r == null) continue;
                String sid = TextSanitizer.normalizeId(r.getShipmentId());
                if (shipmentId.equals(sid)) out.add(r);
            }
        }
        return out;
    }

    private List<ShipmentItemDocument> selectRowsForTotals(List<ShipmentItemDocument> allRows) {
        if (allRows == null || allRows.isEmpty()) return List.of();

        boolean hasSum = allRows.stream().anyMatch(r -> r != null && Boolean.TRUE.equals(r.getIsConsolidatedSum()));
        if (hasSum) {
            return allRows.stream().filter(r -> r != null && Boolean.TRUE.equals(r.getIsConsolidatedSum())).toList();
        }
        return allRows.stream().filter(r -> r != null && !Boolean.TRUE.equals(r.getIsConsolidatedSum())).toList();
    }

    private String resolveCountry(List<ShipmentItemDocument> rows) {
        if (rows == null) return "INT";
        for (ShipmentItemDocument r : rows) {
            if (r == null) continue;
            String c = r.getCountry() == null ? "" : r.getCountry().trim();
            if (!c.isBlank()) return c;
        }
        return "INT";
    }

    private boolean hasConsolidatedSumRows(Map<String, List<ShipmentItemDocument>> byCountry, String shipmentId) {
        if (byCountry == null || byCountry.isEmpty()) return false;

        for (List<ShipmentItemDocument> rows : byCountry.values()) {
            if (rows == null) continue;
            for (ShipmentItemDocument r : rows) {
                if (r == null) continue;
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
            String currency = (s.getCharges() != null && s.getCharges().getCurrency() != null && !s.getCharges().getCurrency().isBlank())
                    ? s.getCharges().getCurrency()
                    : "EUR";
            s.setOrderCharges(zeroCharges(currency));
        }
    }

    private Charges zeroCharges(String currency) {
        String cur = (currency == null || currency.isBlank()) ? "EUR" : currency;
        return Charges.builder()
                .freightCostSystem(BigDecimal.ZERO)
                .dieselFee(BigDecimal.ZERO)
                .tollCharge(BigDecimal.ZERO)
                .expressNextDay(BigDecimal.ZERO)
                .palletExchange(BigDecimal.ZERO)
                .phoneAvis(BigDecimal.ZERO)
                .liftingPlatformSurcharge(BigDecimal.ZERO)
                .custom1(BigDecimal.ZERO)
                .custom2(BigDecimal.ZERO)
                .custom3(BigDecimal.ZERO)
                .custom4(BigDecimal.ZERO)
                .custom5(BigDecimal.ZERO)
                .insurance(BigDecimal.ZERO)
                .dangerousGoodsSurcharge(BigDecimal.ZERO)
                .securityFee(BigDecimal.ZERO)
                .longGoodsSurcharge(BigDecimal.ZERO)
                .currency(cur)
                .build();
    }

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
}