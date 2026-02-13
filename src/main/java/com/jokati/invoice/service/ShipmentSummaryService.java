package com.jokati.invoice.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.jokati.invoice.model.DieselFloater;
import com.jokati.invoice.model.ShipmentItemDocument;
import com.jokati.invoice.model.ShipperExtraCosts;
import com.jokati.invoice.model.ShipperFreightCalculationBasis;
import com.jokati.invoice.model.ShipperRates;
import com.jokati.invoice.repository.ProjectShipmentRepository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * ShipmentSummaryService
 *
 * Mirrors React/TypeScript summary.ts logic in one Spring service:
 *  - prepare rows (per-row calculation hook)
 *  - group by country
 *  - sort by ShipmentId (numeric-first)
 *  - consolidate repeated ShipmentIds (sum row with computed extras, Diesel Floater)
 *  - compute country totals & overall total shipment price
 */
@Service
@Slf4j
public class ShipmentSummaryService {

    private final ProjectShipmentRepository projectShipmentRepository;

    // Inject your services
    private final ShipperFreightCalculationBasisService freightService;
    private final ShipperRatesService ratesService;
    private final ShipperExtraCostsService extraCostsService;
    private final DieselFloaterService dieselService;

    public ShipmentSummaryService(ProjectShipmentRepository projectShipmentRepository,
                                  ShipperFreightCalculationBasisService freightService,
                                  ShipperRatesService ratesService,
                                  ShipperExtraCostsService extraCostsService,
                                  DieselFloaterService dieselService) {
        this.projectShipmentRepository = projectShipmentRepository;
        this.freightService = freightService;
        this.ratesService = ratesService;
        this.extraCostsService = extraCostsService;
        this.dieselService = dieselService;
    }

    // ---------------------------------------------------------------------
    // Dictionaries & Constants (TS parity)
    // ---------------------------------------------------------------------

    /** Mapping from row boolean flags to ExtraCost "Term" labels (German). */
    private static final Map<String, String> EXTRA_COST_TERM_MAP = Map.ofEntries(
            Map.entry("ExpressNextDay", "Express Next Day"),
            Map.entry("Express12", "Express 12:00 Uhr"),
            Map.entry("Express10", "Express 10:00 Uhr"),
            Map.entry("Express8", "Express 08:00 Uhr"),
            Map.entry("Fixtermin", "Fixtermin"),
            Map.entry("EmailAvis", "E-Mail Avis"),
            Map.entry("PhoneAvis", "Telefonisches Avis"),
            Map.entry("BookingInAvis", "Booking in Avis"),
            Map.entry("DangerousGoodsSurcharge", "Gefahrgutzuschlag"),
            Map.entry("LongGoodsSurcharge", "Langgutzuschlag"),
            Map.entry("ShortWeekSurcharge", "Kurzwochenzuschlag"),
            Map.entry("PalletExchange", "Palettentausch"),
            Map.entry("PalletBoxExchange", "Gitterboxtausch"),
            Map.entry("CarrierCertificate", "Spediteurbescheinigung"),
            Map.entry("B2CNationalSurcharge", "B2C Zuschlag (national)"),
            Map.entry("B2CInternationalSurcharge", "B2C Zuschlag (international)"),
            Map.entry("SecurityFee", "Security Fee"),
            Map.entry("Insurance", "Versicherung"),
            Map.entry("PortiPapiere", "Porti/Papiere"),
            Map.entry("Custom1", "Eigene 1"),
            Map.entry("Custom2", "Eigene 2"),
            Map.entry("Custom3", "Eigene 3"),
            Map.entry("Custom4", "Eigene 4"),
            Map.entry("Custom5", "Eigene 5")
    );

    /** Keys whose numeric values are summed in consolidation. */
    private static final List<String> SUM_PROPERTIES = List.of(
            "PalletCount", "EffectiveWeight", "LoadingMeters",
            "WeightByLoadingMeters", "WeightByCubicMeters", "MinimumWeight",
            "ChargeableWeight", "Price"
    );

    /** Tariffs that allow Min/Max application (parity with TS). */
    private static final Set<String> ALLOWED_MIN_MAX_TARIFFS = Set.of(
            "Gewicht 100Kg", "Gewicht 100Kg aufgerundet", "Gewicht 10Kg aufgerundet", "Kilogramm"
    );

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * Prepare consolidated summary for the entire project.
     *
     * @param projectId project id (hex string)
     * @return summary result or null
     */
    public SummaryInitResult getSummaryInit(String projectId) {
        try {
            final String projectIdStr = projectId;
            log.info("getSummaryInit: projectId={}", projectIdStr);

            var projectOpt = projectShipmentRepository.findByProjectId(projectIdStr);
            if (projectOpt.isEmpty()) {
                log.warn("getSummaryInit: no project for projectId={}", projectIdStr);
                return null;
            }
            var project = projectOpt.get();
            List<ShipmentItemDocument> rawRows =
                    Optional.ofNullable(project.getShipmentData()).orElseGet(ArrayList::new);

            Map<String, Object> freightBasis        = fetchFreightBasis(projectIdStr);
            Map<String, Object> rates               = fetchRates(projectIdStr);
            Map<String, Object> extraCosts          = fetchExtraCosts(projectIdStr);
            Map<String, Object> dieselFloaterMatrix = fetchDieselFloaterMatrix();

            List<ShipmentItemDocument> calculatedRows = rawRows.stream()
                    .map(r -> calculateRow(r, freightBasis, rates, extraCosts, dieselFloaterMatrix))
                    .collect(Collectors.toList());

            Map<String, List<ShipmentItemDocument>> preparedByCountry = calculatedRows.stream()
                    .collect(Collectors.groupingBy(row -> Optional.ofNullable(row.getCountry()).orElse("INT"),
                            LinkedHashMap::new, Collectors.toList()));

            Map<String, List<ShipmentItemDocument>> consolidatedByCountry =
                    createSortedConsolidatedShipmentData(preparedByCountry, rates,extraCosts, dieselFloaterMatrix);

            Map<String, RowSummedTotal> countriesTotals = createCountryRowTotal(consolidatedByCountry, extraCosts);
            double overallTotalPrice = addTotalShipmentPrice(countriesTotals);

            SummaryInitResult dto = new SummaryInitResult();
            dto.setConsolidatedShipmentData(consolidatedByCountry);
            dto.setShipmentTotalSummary(new ShipmentTotalSummary(countriesTotals, overallTotalPrice));
            dto.setFetchedShipperExtraCosts(extraCosts);
            dto.setDieselFloaterMatrix(dieselFloaterMatrix);

            log.info("getSummaryInit: success total={}", overallTotalPrice);
            return dto;
        } catch (Exception ex) {
            log.error("getSummaryInit: error -> {}", ex.getMessage(), ex);
            return null;
        }
    }

    public SummaryInitResult getSummaryInitWithMessage(String projectId) {
        try {
            log.info("getSummaryInitWithMessage: projectId={}", projectId);

            var projectOpt = projectShipmentRepository.findByProjectId(projectId);
            if (projectOpt.isEmpty()) {
                log.warn("getSummaryInitWithMessage: no project for projectId={}", projectId);
                return null;
            }

            var project = projectOpt.get();

            List<ShipmentItemDocument> rawRows =
                    Optional.ofNullable(project.getShipmentData())
                            .orElseGet(ArrayList::new);

            Map<String, Object> freightBasis        = fetchFreightBasis(projectId);
            Map<String, Object> rates               = fetchRates(projectId);
            Map<String, Object> extraCosts          = fetchExtraCosts(projectId);
            Map<String, Object> dieselFloaterMatrix = fetchDieselFloaterMatrix();

            List<ShipmentItemDocument> calculatedRows = rawRows.stream()
                    .map(r -> calculateRow(r, freightBasis, rates, extraCosts, dieselFloaterMatrix))
                    .collect(Collectors.toList());

            List<ShipmentItemDocument> filteredRows = calculatedRows.stream()
                    .filter(item -> item.getMessage() != null && !item.getMessage().trim().isEmpty())
                    .collect(Collectors.toList());

            Map<String, List<ShipmentItemDocument>> preparedByCountry =
                    filteredRows.stream()
                            .collect(Collectors.groupingBy(
                                    row -> Optional.ofNullable(row.getCountry()).orElse("INT"),
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

            Map<String, List<ShipmentItemDocument>> consolidatedByCountry =
                    createSortedConsolidatedShipmentData(preparedByCountry, rates, extraCosts, dieselFloaterMatrix);

            Map<String, RowSummedTotal> countriesTotals =
                    createCountryRowTotal(consolidatedByCountry, extraCosts);

            double overallTotalPrice = addTotalShipmentPrice(countriesTotals);

            SummaryInitResult dto = new SummaryInitResult();
            dto.setConsolidatedShipmentData(consolidatedByCountry);
            dto.setShipmentTotalSummary(new ShipmentTotalSummary(countriesTotals, overallTotalPrice));
            dto.setFetchedShipperExtraCosts(extraCosts);
            dto.setDieselFloaterMatrix(dieselFloaterMatrix);

            return dto;

        } catch (Exception ex) {
            log.error("getSummaryInitWithMessage: error", ex);
            return null;
        }
    }

    /**
     * Prepare consolidated summary filtered to one ShipmentId.
     *
     * @param projectId  project ObjectId
     * @param shipmentId ShipmentId to filter
     * @return summary result or null
     */
    public SummaryInitResult getSummaryByShipmentId(ObjectId projectId, String shipmentId) {
        try {
            final String projectIdStr = projectId.toHexString();

            // ✅ FIX: normalize incoming shipmentId to remove hidden newlines/spaces
            final String sid = normalizeShipmentId(shipmentId);

            log.info("getSummaryByShipmentId: projectId={} shipmentId(raw)='{}' normalized='{}'",
                    projectIdStr, shipmentId, sid);

            if (sid == null || sid.isBlank()) {
                log.warn("getSummaryByShipmentId: blank shipmentId");
                return null;
            }

            var projectOpt = projectShipmentRepository.findByProjectId(projectIdStr);
            if (projectOpt.isEmpty()) {
                log.warn("getSummaryByShipmentId: no project for projectId={}", projectIdStr);
                return null;
            }
            var project = projectOpt.get();

            List<ShipmentItemDocument> rawRows =
                    Optional.ofNullable(project.getShipmentData()).orElseGet(ArrayList::new);

            // ✅ FIX: normalize DB shipmentId too before comparison
            List<ShipmentItemDocument> filteredRows = rawRows.stream()
                    .filter(r -> sid.equals(normalizeShipmentId(r.getShipmentId())))
                    .collect(Collectors.toList());

            if (filteredRows.isEmpty()) {
                log.warn("getSummaryByShipmentId: no rows for sid={}", sid);
                return null;
            }

            Map<String, Object> freightBasis        = fetchFreightBasis(projectIdStr);
            Map<String, Object> rates               = fetchRates(projectIdStr);
            Map<String, Object> extraCosts          = fetchExtraCosts(projectIdStr);
            Map<String, Object> dieselFloaterMatrix = fetchDieselFloaterMatrix();

            List<ShipmentItemDocument> calculatedRows = filteredRows.stream()
                    .map(r -> calculateRow(r, freightBasis, rates, extraCosts, dieselFloaterMatrix))
                    .collect(Collectors.toList());

            Map<String, List<ShipmentItemDocument>> preparedByCountry = calculatedRows.stream()
                    .collect(Collectors.groupingBy(row -> Optional.ofNullable(row.getCountry()).orElse("INT"),
                            LinkedHashMap::new, Collectors.toList()));

            Map<String, List<ShipmentItemDocument>> consolidatedByCountry =
                    createSortedConsolidatedShipmentData(preparedByCountry,rates, extraCosts, dieselFloaterMatrix);

            Map<String, RowSummedTotal> countriesTotals = createCountryRowTotal(consolidatedByCountry, extraCosts);
            double overallTotalPrice = addTotalShipmentPrice(countriesTotals);

            SummaryInitResult dto = new SummaryInitResult();
            dto.setConsolidatedShipmentData(consolidatedByCountry);
            dto.setShipmentTotalSummary(new ShipmentTotalSummary(countriesTotals, overallTotalPrice));
            dto.setFetchedShipperExtraCosts(extraCosts);
            dto.setDieselFloaterMatrix(dieselFloaterMatrix);

            log.info("getSummaryByShipmentId: success total={}", overallTotalPrice);
            return dto;
        } catch (Exception ex) {
            log.error("getSummaryByShipmentId: error -> {}", ex.getMessage(), ex);
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // FIX helper (added)
    // ---------------------------------------------------------------------

    /**
     * ✅ Normalizes shipmentId to avoid mismatch due to hidden whitespace/newlines/NBSP.
     * Example DB value might be "\n2138710242" while input is "2138710242".
     * This method removes all whitespace and NBSP.
     *
     * NOTE: This is not logic change; it's only making equality work as intended.
     */
    private static String normalizeShipmentId(Object shipmentId) {
        if (shipmentId == null) return null;

        String s = String.valueOf(shipmentId);

        // strip() is Java 11+; removes Unicode whitespace at ends
        try {
            s = s.strip();
        } catch (Throwable t) {
            // fallback for older Java
            s = s.trim();
        }

        // remove NBSP
        s = s.replace("\u00A0", "");

        // remove all remaining whitespace (space/tab/newline)
        s = s.replaceAll("\\s+", "");

        return s.isEmpty() ? null : s;
    }

    // ---------------------------------------------------------------------
    // Fetch helpers
    // ---------------------------------------------------------------------

    private Map<String, Object> fetchFreightBasis(String projectIdHex) {
        try {
            Optional<ShipperFreightCalculationBasis> opt = freightService.findByProjectId(projectIdHex);
            Map<String, Object> countries = opt.map(ShipperFreightCalculationBasis::getCountries).orElseGet(HashMap::new);
            log.debug("fetchFreightBasis: keys={}", countries.keySet());
            return countries != null ? countries : new HashMap<>();
        } catch (Exception ex) {
            log.error("fetchFreightBasis: error -> {}", ex.getMessage(), ex);
            return new HashMap<>();
        }
    }

    private Map<String, Object> fetchRates(String projectIdHex) {
        try {
            Optional<ShipperRates> opt = ratesService.findByProjectId(projectIdHex);
            Map<String, Object> rates = opt.map(ShipperRates::getRates).orElseGet(HashMap::new);
            log.debug("fetchRates: keys={}", rates.keySet());
            return rates != null ? rates : new HashMap<>();
        } catch (Exception ex) {
            log.error("fetchRates: error -> {}", ex.getMessage(), ex);
            return new HashMap<>();
        }
    }

    private Map<String, Object> fetchExtraCosts(String projectIdHex) {
        try {
            Optional<ShipperExtraCosts> opt = extraCostsService.findByProjectId(projectIdHex);
            Map<String, Object> extraCosts = opt.map(ShipperExtraCosts::getExtraCosts).orElseGet(HashMap::new);
            log.debug("fetchExtraCosts: keys={}", extraCosts.keySet());
            return extraCosts != null ? extraCosts : new HashMap<>();
        } catch (Exception ex) {
            log.error("fetchExtraCosts: error -> {}", ex.getMessage(), ex);
            return new HashMap<>();
        }
    }

    /** Diesel Floater matrix: years → List(months) → Map(source→value). Picks latest document by ObjectId timestamp. */
    private Map<String, Object> fetchDieselFloaterMatrix() {
        try {
            List<DieselFloater> all = dieselService.findAll();
            if (all == null || all.isEmpty()) {
                log.warn("fetchDieselFloaterMatrix: none found");
                return new HashMap<>();
            }
            DieselFloater latest = all.stream()
                    .max(Comparator.comparingInt(df -> safeObjectIdTimestamp(df.getId())))
                    .orElse(all.get(0));

            Map<String, Object> years = latest.getYears();
            log.debug("fetchDieselFloaterMatrix: picked id={} years keys={}", latest.getId(),
                    years != null ? years.keySet() : List.of());
            return years != null ? years : new HashMap<>();
        } catch (Exception ex) {
            log.error("fetchDieselFloaterMatrix: error -> {}", ex.getMessage(), ex);
            return new HashMap<>();
        }
    }

    private int safeObjectIdTimestamp(String id) {
        try {
            return new ObjectId(id).getTimestamp();
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    // ---------------------------------------------------------------------
    // Sorting & Consolidation
    // ---------------------------------------------------------------------

    public Map<String, List<ShipmentItemDocument>> createSortedConsolidatedShipmentData(
            Map<String, List<ShipmentItemDocument>> groupedByCountry,
            Map<String, Object> rates,
            Map<String, Object> extraCosts,
            Map<String, Object> dieselFloaterMatrix
    ) {
        Map<String, List<ShipmentItemDocument>> result = new LinkedHashMap<>();
        if (groupedByCountry == null || groupedByCountry.isEmpty()) return result;

        for (Map.Entry<String, List<ShipmentItemDocument>> e : groupedByCountry.entrySet()) {
            String country = e.getKey();
            List<ShipmentItemDocument> rows = e.getValue();

            List<ShipmentItemDocument> sorted = sortByShipmentId(rows);
            List<ShipmentItemDocument> consolidated =
                    createConsolidatedRows(sorted, country, rates, extraCosts, dieselFloaterMatrix);

            result.put(country, consolidated);
        }

        return result;
    }

    private List<ShipmentItemDocument> sortByShipmentId(List<ShipmentItemDocument> shipmentData) {
        if (shipmentData == null) return Collections.emptyList();

        return shipmentData.stream().sorted((a, b) -> {
            String aId = Optional.ofNullable(a.getShipmentId()).orElse("");
            String bId = Optional.ofNullable(b.getShipmentId()).orElse("");

            boolean aPresent = !aId.isBlank();
            boolean bPresent = !bId.isBlank();
            if (!aPresent && !bPresent) return 0;
            if (!aPresent) return 1;
            if (!bPresent) return -1;

            boolean aNumeric = aId.matches("\\d+");
            boolean bNumeric = bId.matches("\\d+");

            if (aNumeric && bNumeric) {
                return Long.compare(Long.parseLong(aId), Long.parseLong(bId));
            }
            if (aNumeric) return -1;
            if (bNumeric) return 1;
            return aId.compareToIgnoreCase(bId);
        }).collect(Collectors.toList());
    }

    private List<ShipmentItemDocument> createConsolidatedRows(
            List<ShipmentItemDocument> rowsByCountry,
            String country,
            Map<String, Object> rates,
            Map<String, Object> extraCosts,
            Map<String, Object> dieselFloaterMatrix
    ) {
        log.info("dieselFloaterMatrix is {}", dieselFloaterMatrix.isEmpty());
        Map<String, Map<String, Double>> sumsPerShipment = new LinkedHashMap<>();
        Map<String, Integer> lastIndexPerShipment = new LinkedHashMap<>();
        List<ShipmentItemDocument> out = new ArrayList<>();

        List<String> consolidatedShipmentIds = getConsolidatedShipmentIds(rowsByCountry);

        for (int idx = 0; idx < rowsByCountry.size(); idx++) {
            ShipmentItemDocument row = rowsByCountry.get(idx);
            String sid = row.getShipmentId();

            if (sid != null && !sid.isBlank()) {
                Map<String, Double> sums = sumsPerShipment.computeIfAbsent(sid, k -> new HashMap<>());
                SUM_PROPERTIES.forEach(prop -> addNumeric(sums, prop, getNumeric(row, prop)));

                boolean isGroup = consolidatedShipmentIds.contains(sid);
                row.setIsConsolidated(isGroup);
                if (isGroup) lastIndexPerShipment.put(sid, idx);
            }

            out.add(row);
        }

        for (int i = 0; i < consolidatedShipmentIds.size(); i++) {

            String sid = consolidatedShipmentIds.get(i);
            log.info("consolidated shipment id " + sid);
            int lastIndex = lastIndexPerShipment.getOrDefault(sid, -1);
            if (lastIndex < 0) continue;

            List<ShipmentItemDocument> groupRows = out.stream()
                    .filter(r -> sid.equals(r.getShipmentId()))
                    .collect(Collectors.toList());

            double maxLength = groupRows.stream().mapToDouble(r -> opt(r.getLength())).max().orElse(0);
            boolean hasFP = groupRows.stream().anyMatch(r -> "FP".equalsIgnoreCase(r.getPackagingType()));
            int fpPalletCount = groupRows.stream()
                    .filter(r -> "FP".equalsIgnoreCase(r.getPackagingType()))
                    .mapToInt(r -> optInt(r.getPalletCount()))
                    .sum();

            int insertIndex = lastIndex + 1 + i;
            while (insertIndex < out.size() && sid.equals(out.get(insertIndex).getShipmentId())) {
                insertIndex++;
            }

            ShipmentItemDocument base = rowsByCountry.get(lastIndex);
            Map<String, Double> sums = sumsPerShipment.getOrDefault(sid, Collections.emptyMap());

            ShipmentItemDocument sumRow = ShipmentItemDocument.builder()
                    .idUpper(null)
                    .idLower(UUID.randomUUID().toString())
                    .shipmentId(sid)
                    .shipmentDate(base.getShipmentDate())
                    .zipCodeShipper(base.getZipCodeShipper())
                    .zipCodeConsignee(base.getZipCodeConsignee())
                    .city(base.getCity())
                    .country(base.getCountry())
                    .length(maxLength)
                    .wide(0.0)
                    .height(0.0)
                    .loadingMeters(sums.getOrDefault("LoadingMeters", 0.0))
                    .cubicMeters(0.0)
                    .palletCount(sums.getOrDefault("PalletCount", 0.0) == 0.0 ? 0 : sums.get("PalletCount").intValue())
                    .packagingType("CC")
                    .effectiveWeight(sums.getOrDefault("EffectiveWeight", 0.0))
                    .chargeableWeight(sums.getOrDefault("ChargeableWeight", 0.0))
                    .minimumWeight(sums.getOrDefault("MinimumWeight", 0.0))
                    .weightByCubicMeters(sums.getOrDefault("WeightByCubicMeters", 0.0))
                    .weightByLoadingMeters(sums.getOrDefault("WeightByLoadingMeters", 0.0))
                    .stackable(Boolean.FALSE)
                    .stackFactor(1)
                    .stackId("")
                    .stackFootprintLoadingMeters(0.0)
                    .message("")
                    .errorType(0)
                    .price(sums.getOrDefault("Price", 0.0)) // recalculated inside calculateRow
                    .totalPrice(0.0)
                    .extraCostsTotalPrice(0.0)
                    .toll(0.0)
                    .tollPercent(0.0)
                    .diesel(0.0)
                    .dieselPercent(0.0)
                    .isConsolidated(Boolean.FALSE)
                    .isConsolidatedSum(Boolean.TRUE)
                    .hasPackagingType(hasFP)
                    .hasFP(hasFP)                 // requires model fields
                    .fpPalletCount(fpPalletCount) // requires model fields
                    .projectType(base.getProjectType())
                    .undefined(base.getUndefined())
                    .build();

            if (country.isBlank()) country = "INT";
            sumRow = calculateRow(
                    sumRow,
                    Map.of(base.getCountry(), Map.of("IsConsolidated", true)),
                    rates,
                    Map.of(country, extraCosts.get(country)),
                    dieselFloaterMatrix
            );

            if (insertIndex >= out.size()) {
                out.add(sumRow);
            } else {
                out.add(insertIndex, sumRow);
            }
            log.debug("createConsolidatedRows: inserted sum row for ShipmentId={} at {}", sid, insertIndex);
        }

        log.info("createConsolidatedRows: country={} sumRows={}", country, consolidatedShipmentIds.size());
        return out;
    }

    private List<String> getConsolidatedShipmentIds(List<ShipmentItemDocument> rows) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        rows.forEach(r -> {
            String sid = r.getShipmentId();
            if (sid != null && !sid.isBlank()) counts.put(sid, counts.getOrDefault(sid, 0) + 1);
        });
        return counts.entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    // Country totals & overall total
    // ---------------------------------------------------------------------

    public Map<String, RowSummedTotal> createCountryRowTotal(
            Map<String, List<ShipmentItemDocument>> groupedByCountry,
            Map<String, Object> extraCostsCatalog
    ) {
        Map<String, RowSummedTotal> totals = new LinkedHashMap<>();
        if (groupedByCountry == null || groupedByCountry.isEmpty()) return totals;

        for (Map.Entry<String, List<ShipmentItemDocument>> e : groupedByCountry.entrySet()) {
            String country = e.getKey();
            List<ShipmentItemDocument> rows = e.getValue();

            List<ShipmentItemDocument> baseRows = rows.stream()
                    .filter(r -> !Boolean.TRUE.equals(r.getIsConsolidated()))
                    .collect(Collectors.toList());
            List<ShipmentItemDocument> sumRows = rows.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsConsolidatedSum()))
                    .collect(Collectors.toList());

            boolean consolidated = !sumRows.isEmpty();
            int shipmentDataLength = consolidated
                    ? (int) rows.stream().filter(r -> Boolean.TRUE.equals(r.getIsConsolidatedSum()) || !Boolean.TRUE.equals(r.getIsConsolidated())).count()
                    : rows.size();

            double netPrice = baseRows.stream().mapToDouble(r -> opt(r.getPrice())).sum();

            RowSummedTotal bucket = new RowSummedTotal();
            bucket.setShipmentDataLength(shipmentDataLength);
            bucket.setPrice(round2(netPrice));

            // Toll unit from extra costs (Maut)
            ExtraTermSpec maut = findExtraTerm(extraCostsCatalog, country, "Maut");
            String tollUnit = maut == null ? "€" : Optional.ofNullable(maut.unit()).orElse("€");

            if ("€".equals(tollUnit)) {
                double toll = baseRows.stream().mapToDouble(r -> opt(r.getToll())).sum();
                bucket.setTollPrice(round2(toll));
            } else if ("%".equals(tollUnit)) {
                double percent = maut.value() / 100.0;
                bucket.setTollPrice(round2(netPrice * percent));
            }

            double diesel = baseRows.stream().mapToDouble(r -> opt(r.getDiesel())).sum();
            bucket.setDieselPrice(round2(diesel));

            // Extras from sum rows (computed there)
            double extrasFromSumRows = sumRows.stream()
                    .mapToDouble(r -> opt(r.getExtraCostsTotalPrice()) - opt(r.getToll()) - opt(r.getDiesel()))
                    .sum();

            double extrasTotal = round2(bucket.getTollPrice() + bucket.getDieselPrice() + extrasFromSumRows);
            bucket.setTotalExtraCostsPrice(extrasTotal);
            bucket.setTotalPrice(round2(bucket.getPrice() + bucket.getTotalExtraCostsPrice()));

            totals.put(country, bucket);
        }

        return totals;
    }

    private double addTotalShipmentPrice(Map<String, RowSummedTotal> byCountry) {
        if (byCountry == null || byCountry.isEmpty()) return 0.0;
        return round2(byCountry.values().stream().mapToDouble(RowSummedTotal::getTotalPrice).sum());
    }

    // ---------------------------------------------------------------------
    // Row calculation (TS parity + Diesel Floater)
    // ---------------------------------------------------------------------

//    private ShipmentItemDocument calculateRow(
//            ShipmentItemDocument row,
//            Map<String, Object> freightBasis,
//            Map<String, Object> rates,
//            Map<String, Object> extraCosts,
//            Map<String, Object> dieselFloaterMatrix
//    ) {
//        try {
//            log.info("dieselFloaterMatrix value under {}", dieselFloaterMatrix.isEmpty());
//            // Country normalize
//            final boolean isSumRow = Boolean.TRUE.equals(row.getIsConsolidatedSum());
//            
//            // Country normalize
//            String country = safeString(row.getCountry());
//            if (country.isBlank()) country = "INT";
//            row.setCountry(country);
//
//            // Stack/Loading meters
//            boolean stackable = Boolean.TRUE.equals(row.getStackable());
//            int stackFactor = resolveRowStackFactor(row.getStackFactor(), stackable);
//            row.setStackable(stackable);
//            row.setStackFactor(stackFactor);
//
//            double loadingMeters = safeDouble(row.getLoadingMeters());
//            if (loadingMeters <= 0) {
//                loadingMeters = toFixed(((safeDouble(row.getLength()) / 100.0) * (safeDouble(row.getWide()) / 100.0) / 2.4) * safeInt(row.getPalletCount()), 3);
//            }
//            if (stackable && stackFactor > 1) {
//                loadingMeters = toFixed(loadingMeters / stackFactor, 3);
//            }
//            row.setLoadingMeters(loadingMeters);
//            row.setStackFootprintLoadingMeters(loadingMeters);
//
//            double cubicMeters;
//            if (isZero(row.getLength()) && isZero(row.getWide()) && isZero(row.getHeight())) {
//                cubicMeters = safeDouble(row.getCubicMeters());
//            } else {
//                double l = safeDouble(row.getLength()) / 100.0;
//                double w = safeDouble(row.getWide()) / 100.0;
//                double h = safeDouble(row.getHeight()) / 100.0;
//                cubicMeters = toFixed(l * w * h * safeInt(row.getPalletCount()), 3);
//            }
//            row.setCubicMeters(cubicMeters);
//
//            // Freight basis
//            Map<String, Object> basisForCountry = getCountryNode(freightBasis, country);
//            int calculationType = safeInt(basisForCountry.getOrDefault("CalculationType", 0));
//            Map<String, Object> bulkiness = getNode(basisForCountry, "Bulkiness");
//            Map<String, Object> minWeight = getNode(basisForCountry, "MinimumWeight");
//            Map<String, Object> advancedOptions = firstNonNull(
//                    getNode(basisForCountry, "advancedOptions"),
//                    getNode(basisForCountry, "AdvancedOptions")
//            );
//
//            double kgThreshold = toDouble(advancedOptions.get("LoadingMetersKg"));
//            double ldmThreshold = toDouble(advancedOptions.get("LoadingMetersLdm"));
//
//            boolean isSumRow = Boolean.TRUE.equals(row.getIsConsolidatedSum());
//            double effForCheck = isSumRow
//                    ? Math.max(safeDouble(row.getEffectiveWeight()), safeDouble(row.getChargeableWeight()))
//                    : safeDouble(row.getEffectiveWeight());
//            boolean meetsKg  = kgThreshold > 0 && effForCheck >= kgThreshold;
//            boolean meetsLdm = ldmThreshold > 0 && loadingMeters >= ldmThreshold;
//            int effectiveCalculationType = (calculationType == 0 || calculationType == 1) && (meetsKg || meetsLdm) ? 2 : calculationType;
//
//            double weightByCubicMeters = 0;
//            if (effectiveCalculationType == 1 && bulkiness != null) {
//                double cmRef = toDouble(bulkiness.get("CubicMeters"));
//                weightByCubicMeters = toFixed(cmRef * cubicMeters, 0);
//            }
//            row.setWeightByCubicMeters(weightByCubicMeters);
//
//            double weightByLoadingMeters = 0;
//            if (effectiveCalculationType == 2 && bulkiness != null) {
//                double lmRef = toDouble(bulkiness.get("LoadingMeters"));
//                weightByLoadingMeters = toFixed(lmRef * loadingMeters, 0);
//            }
//            row.setWeightByLoadingMeters(weightByLoadingMeters);
//
//            double minWeightValue = computeMinimumWeight(minWeight, safeString(row.getPackagingType()), safeInt(row.getPalletCount()));
//            row.setMinimumWeight(minWeightValue);
//
//            double chargeable = Math.max(
//                    Math.max(safeDouble(row.getEffectiveWeight()), weightByCubicMeters),
//                    Math.max(weightByLoadingMeters, minWeightValue)
//            );
//            row.setChargeableWeight(chargeable);
//
//            // Rates & zip zone
//            Map<String, Object> countryRate = getCountryNode(rates, country);
//            if (countryRate == null || countryRate.isEmpty()) {
//                if (!isSumRow) {
//                    setError(row, "Land im Tarif nicht angelegt.", 10);
//                    return row;
//                }
//            }
//
//            if (safeInt(row.getPalletCount()) <= 0 && !isSumRow) {
//                setError(row, "Keine Anzahl Packstücke eingetragen.", 3);
//                return row;
//            }
//            if (isBlank(row.getPackagingType())) {
//                setError(row, "Keine Verpackungsart ausgewählt.", 4);
//                return row;
//            }
//            if (safeDouble(row.getEffectiveWeight()) <= 0 && !isSumRow) {
//                setError(row, "Kein Effektivgewicht eingetragen.", 6);
//                return row;
//            }
//
//            double netPrice = 0.0;
//            if (countryRate != null && !countryRate.isEmpty()) {
//                String rateType = safeString(countryRate.get("TariffType"));
//                double normalizedChargeable = normalizeChargeableWeight(rateType, chargeable, row);
//
//                Map<String, Object> weightsTable = getNode(countryRate, "Weights");
//                String matchedWeightKey = findMatchedWeightKey(weightsTable.keySet(), normalizedChargeable);
//                if (matchedWeightKey == null) {
//                    setError(row, "Kein passendes Gewicht im Tarif gepflegt", 11);
//                    return row;
//                }
//                Map<String, Object> weightRow = getNode(weightsTable, matchedWeightKey);
//
//                int zipCode = resolveZip(row);
//                List<Map<String, Object>> zipCodesList = getZipCodes(countryRate);
//                String zoneId = findZipZoneId(zipCodesList, zipCode);
//                if (zoneId == null) {
//                    setError(row, "Kein Preis für die entsprechende Postleitzahl eingetragen.", 12);
//                    return row;
//                }
//
//                double basePrice = getPriceForZone(weightRow, zoneId, rateType, normalizedChargeable);
//                netPrice = applyMinMaxIfAny(basePrice, findZipRangeById(zipCodesList, zoneId), rateType);
//            } else {
//                netPrice = opt(row.getPrice());
//            }
//
//            // Extra costs & Diesel Floater
//            List<ExtraTermSpec> mergedExtras = getMergedExtraCost(extraCosts, country);
//
//            // Toll (Maut)
//            ExtraTermSpec maut = findTerm(mergedExtras, "Maut");
//            String tollUnit = (maut == null) ? "€" : maut.unit();
//            double tollValue = (maut == null) ? 0.0 : maut.value();
//            double totalToll = "€".equals(tollUnit) ? (safeInt(row.getPalletCount()) * tollValue)
//                                                    : (netPrice * tollValue / 100.0);
//
//            // Diesel Floater config
//            DieselFloaterConfig floaterCfg = extractDieselFloaterConfig(extraCosts, country);
//            double dieselPercent = 0.0;
//            double dieselEuro    = 0.0;
//
//            if (floaterCfg != null) {
//                double matrixVal  = getDieselFloaterMatrixValue(dieselFloaterMatrix, safeString(row.getShipmentDate()), floaterCfg.source());
//                dieselPercent     = getDieselFloaterPercent(floaterCfg.brackets(), matrixVal);
//                dieselEuro        = (netPrice / 100.0) * dieselPercent;
//            } else {
//                // fallback: plain Dieselzuschlag
//                ExtraTermSpec dieselSpec = findTerm(mergedExtras, "Dieselzuschlag");
//                if (dieselSpec != null) {
//                    if ("%".equals(dieselSpec.unit())) {
//                        dieselPercent = dieselSpec.value();
//                        dieselEuro    = (netPrice / 100.0) * dieselPercent;
//                    } else {
//                        dieselEuro    = dieselSpec.value();
//                    }
//                }
//            }
//
//            // Palettentausch parity for consolidated FP sum rows
//            double palletExchange = 0.0;
//            double palletBoxExchange = 0.0;
//
//            ExtraTermSpec pe = findTerm(mergedExtras, "Palettentausch");
//            if (pe != null) {
//                double perPallet = "%".equals(pe.unit()) ? (netPrice * pe.value() / 100.0) : pe.value();
//                if (Boolean.TRUE.equals(row.getIsConsolidatedSum()) && Boolean.TRUE.equals(row.getHasFP())) {
//                    palletExchange = perPallet * optInt(row.getFpPalletCount());
//                } else if ("FP".equalsIgnoreCase(safeString(row.getPackagingType()))) {
//                    palletExchange = perPallet * safeInt(row.getPalletCount());
//                }
//            }
//
//            ExtraTermSpec gb = findTerm(mergedExtras, "Gitterboxtausch");
//            if (gb != null && "GP".equalsIgnoreCase(safeString(row.getPackagingType()))) {
//                double perBox = "%".equals(gb.unit()) ? (netPrice * gb.value() / 100.0) : gb.value();
//                palletBoxExchange = perBox * safeInt(row.getPalletCount());
//            }
//
//            // Flag-driven extras (basic €/%)
//            Map<String, Double> extrasMap = new LinkedHashMap<>();
//            addFlagExtra(extrasMap, mergedExtras, row.getExpressNextDay(), "Express Next Day", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getExpress12(), "Express 12:00 Uhr", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getExpress10(), "Express 10:00 Uhr", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getExpress8(), "Express 08:00 Uhr", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getFixtermin(), "Fixtermin", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getEmailAvis(), "E-Mail Avis", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getPhoneAvis(), "Telefonisches Avis", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getBookingInAvis(), "Booking in Avis", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getDangerousGoodsSurcharge(), "Gefahrgutzuschlag", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getLongGoodsSurcharge(), "Langgutzuschlag", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getShortWeekSurcharge(), "Kurzwochenzuschlag", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getCarrierCertificate(), "Spediteurbescheinigung", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getB2cNationalSurcharge(), "B2C Zuschlag (national)", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getB2cInternationalSurcharge(), "B2C Zuschlag (international)", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getSecurityFee(), "Security Fee", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getInsurance(), "Versicherung", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getPortiPapiere(), "Porti/Papiere", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getCustom1(), "Eigene 1", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getCustom2(), "Eigene 2", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getCustom3(), "Eigene 3", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getCustom4(), "Eigene 4", netPrice);
//            addFlagExtra(extrasMap, mergedExtras, row.getCustom5(), "Eigene 5", netPrice);
//
//            // Totals
//            row.setPrice(round2(netPrice));
//            row.setToll(round2(totalToll));
//            row.setTollPercent(round2(tollValue));
//            row.setDiesel(round2(dieselEuro));
//            row.setDieselPercent(round2(dieselPercent));
//
//            double extrasSum = extrasMap.values().stream().mapToDouble(ShipmentSummaryService::round2).sum()
//                    + round2(palletExchange) + round2(palletBoxExchange);
//
//            double totalExtraCosts = round2(totalToll + dieselEuro + extrasSum);
//            double totalRowPrice   = round2(netPrice + totalExtraCosts);
//
//            row.setExtraCostsTotalPrice(totalExtraCosts);
//            row.setTotalPrice(totalRowPrice);
//
//            // DO NOT RESET MESSAGE IF ERROR EXISTS
//            if (row.getErrorType() == null || row.getErrorType() == 0) {
//                row.setMessage("");
//            }
//
//            log.debug("calculateRow: sid={} country={} net={} toll={} diesel={} extras={} total={}",
//                    safeString(row.getShipmentId()), country, row.getPrice(), row.getToll(), row.getDiesel(),
//                    row.getExtraCostsTotalPrice(), row.getTotalPrice());
//
//            return row;
//        } catch (Exception ex) {
//            log.error("calculateRow: error -> {}", ex.getMessage(), ex);
//            setError(row, "Berechnungsfehler", 99);
//            return row;
//        }
//    }

    private ShipmentItemDocument calculateRow(
            ShipmentItemDocument row,
            Map<String, Object> freightBasis,
            Map<String, Object> rates,
            Map<String, Object> extraCosts,
            Map<String, Object> dieselFloaterMatrix
    ) {
        try {
            log.info("dieselFloaterMatrix value under {}", dieselFloaterMatrix.isEmpty());

            // Consolidated sum row?
            final boolean isSumRow = Boolean.TRUE.equals(row.getIsConsolidatedSum());

            // Country normalize
            String country = safeString(row.getCountry());
            if (country.isBlank()) country = "INT";
            row.setCountry(country);

            // Stack/Loading meters
            boolean stackable = Boolean.TRUE.equals(row.getStackable());
            int stackFactor = resolveRowStackFactor(row.getStackFactor(), stackable);
            row.setStackable(stackable);
            row.setStackFactor(stackFactor);

            double loadingMeters = safeDouble(row.getLoadingMeters());
            if (loadingMeters <= 0) {
                loadingMeters = toFixed(((safeDouble(row.getLength()) / 100.0) * (safeDouble(row.getWide()) / 100.0) / 2.4) * safeInt(row.getPalletCount()), 3);
            }
            if (stackable && stackFactor > 1) {
                loadingMeters = toFixed(loadingMeters / stackFactor, 3);
            }
            row.setLoadingMeters(loadingMeters);
            row.setStackFootprintLoadingMeters(loadingMeters);

            double cubicMeters;
            if (isZero(row.getLength()) && isZero(row.getWide()) && isZero(row.getHeight())) {
                cubicMeters = safeDouble(row.getCubicMeters());
            } else {
                double l = safeDouble(row.getLength()) / 100.0;
                double w = safeDouble(row.getWide()) / 100.0;
                double h = safeDouble(row.getHeight()) / 100.0;
                cubicMeters = toFixed(l * w * h * safeInt(row.getPalletCount()), 3);
            }
            row.setCubicMeters(cubicMeters);

            // Freight basis & advanced options
            Map<String, Object> basisForCountry = getCountryNode(freightBasis, country);
            int calculationType = safeInt(basisForCountry.getOrDefault("CalculationType", 0));
            Map<String, Object> bulkiness = getNode(basisForCountry, "Bulkiness");
            Map<String, Object> minWeight = getNode(basisForCountry, "MinimumWeight");
            Map<String, Object> advancedOptions = firstNonNull(
                    getNode(basisForCountry, "advancedOptions"),
                    getNode(basisForCountry, "AdvancedOptions")
            );

            double kgThreshold  = toDouble(advancedOptions.get("LoadingMetersKg"));
            double ldmThreshold = toDouble(advancedOptions.get("LoadingMetersLdm"));

            // Switch to LM calculation based on thresholds; for sum rows we won't derive weightBy*
            double effForCheck = isSumRow
                    ? Math.max(safeDouble(row.getEffectiveWeight()), safeDouble(row.getChargeableWeight()))
                    : safeDouble(row.getEffectiveWeight());
            boolean meetsKg  = kgThreshold > 0 && effForCheck >= kgThreshold;
            boolean meetsLdm = ldmThreshold > 0 && loadingMeters >= ldmThreshold;
            int effectiveCalculationType = (calculationType == 0 || calculationType == 1) && (meetsKg || meetsLdm) ? 2 : calculationType;

            // ===== For SUM ROWS: freeze derived weights and minimum weight =====
            double weightByCubicMeters = 0.0;
            double weightByLoadingMeters = 0.0;
            double minWeightValue = 0.0;

            if (!isSumRow) {
                if (effectiveCalculationType == 1 && bulkiness != null) {
                    double cmRef = toDouble(bulkiness.get("CubicMeters"));
                    weightByCubicMeters = toFixed(cmRef * cubicMeters, 0);
                }
                if (effectiveCalculationType == 2 && bulkiness != null) {
                    double lmRef = toDouble(bulkiness.get("LoadingMeters"));
                    weightByLoadingMeters = toFixed(lmRef * loadingMeters, 0);
                }
                minWeightValue = computeMinimumWeight(minWeight, safeString(row.getPackagingType()), safeInt(row.getPalletCount()));
            }
            row.setWeightByCubicMeters(weightByCubicMeters);
            row.setWeightByLoadingMeters(weightByLoadingMeters);
            row.setMinimumWeight(minWeightValue);

            // ===== Preserve incoming chargeable for SUM ROWS; base rows compute normally =====
            double chargeable = (isSumRow && row.getChargeableWeight() != null && row.getChargeableWeight() > 0)
                    ? row.getChargeableWeight()
                    : Math.max(
                          Math.max(safeDouble(row.getEffectiveWeight()), weightByCubicMeters),
                          Math.max(weightByLoadingMeters, minWeightValue)
                      );
            row.setChargeableWeight(chargeable);

            // Rates & zip zone
            Map<String, Object> countryRate = getCountryNode(rates, country);
            if (countryRate == null || countryRate.isEmpty()) {
                if (!isSumRow) {
                    setError(row, "Land im Tarif nicht angelegt.", 10);
                    return row;
                }
            }

            if (safeInt(row.getPalletCount()) <= 0 && !isSumRow) {
                setError(row, "Keine Anzahl Packstücke eingetragen.", 3);
                return row;
            }
            if (isBlank(row.getPackagingType())) {
                setError(row, "Keine Verpackungsart ausgewählt.", 4);
                return row;
            }
            if (safeDouble(row.getEffectiveWeight()) <= 0 && !isSumRow) {
                setError(row, "Kein Effektivgewicht eingetragen.", 6);
                return row;
            }

            double netPrice = 0.0;
            if (countryRate != null && !countryRate.isEmpty()) {
                String rateType = safeString(countryRate.get("TariffType"));
                double normalizedChargeable = normalizeChargeableWeight(rateType, chargeable, row);

                Map<String, Object> weightsTable = getNode(countryRate, "Weights");
                String matchedWeightKey = findMatchedWeightKey(weightsTable.keySet(), normalizedChargeable);
                if (matchedWeightKey == null) {
                    setError(row, "Kein passendes Gewicht im Tarif gepflegt", 11);
                    return row;
                }
                Map<String, Object> weightRow = getNode(weightsTable, matchedWeightKey);

                int zipCode = resolveZip(row);
                List<Map<String, Object>> zipCodesList = getZipCodes(countryRate);
                String zoneId = findZipZoneId(zipCodesList, zipCode);
                if (zoneId == null) {
                    setError(row, "Kein Preis für die entsprechende Postleitzahl eingetragen.", 12);
                    return row;
                }

                double basePrice = getPriceForZone(weightRow, zoneId, rateType, normalizedChargeable);
                netPrice = applyMinMaxIfAny(basePrice, findZipRangeById(zipCodesList, zoneId), rateType);
            } else {
                // fallback to incoming price for sum rows if no rate
                netPrice = opt(row.getPrice());
            }

            // Extra costs & Diesel Floater
            List<ExtraTermSpec> mergedExtras = getMergedExtraCost(extraCosts, country);

            // Toll (Maut)
            ExtraTermSpec maut = findTerm(mergedExtras, "Maut");
            String tollUnit = (maut == null) ? "€" : maut.unit();
            double tollValue = (maut == null) ? 0.0 : maut.value();
            double totalToll = "€".equals(tollUnit)
                    ? (safeInt(row.getPalletCount()) * tollValue)
                    : (netPrice * tollValue / 100.0);

            // Diesel Floater config
            DieselFloaterConfig floaterCfg = extractDieselFloaterConfig(extraCosts, country);
            double dieselPercent = 0.0;
            double dieselEuro    = 0.0;

            if (floaterCfg != null) {
                double matrixVal  = getDieselFloaterMatrixValue(dieselFloaterMatrix, safeString(row.getShipmentDate()), floaterCfg.source());
                dieselPercent     = getDieselFloaterPercent(floaterCfg.brackets(), matrixVal);
                dieselEuro        = (netPrice / 100.0) * dieselPercent;
            } else {
                // fallback: plain Dieselzuschlag
                ExtraTermSpec dieselSpec = findTerm(mergedExtras, "Dieselzuschlag");
                if (dieselSpec != null) {
                    if ("%".equals(dieselSpec.unit())) {
                        dieselPercent = dieselSpec.value();
                        dieselEuro    = (netPrice / 100.0) * dieselPercent;
                    } else {
                        dieselEuro    = dieselSpec.value();
                    }
                }
            }

            // Palettentausch parity for consolidated FP sum rows
            double palletExchange = 0.0;
            double palletBoxExchange = 0.0;

            ExtraTermSpec pe = findTerm(mergedExtras, "Palettentausch");
            if (pe != null) {
                double perPallet = "%".equals(pe.unit()) ? (netPrice * pe.value() / 100.0) : pe.value();
                if (Boolean.TRUE.equals(row.getIsConsolidatedSum()) && Boolean.TRUE.equals(row.getHasFP())) {
                    palletExchange = perPallet * optInt(row.getFpPalletCount());
                } else if ("FP".equalsIgnoreCase(safeString(row.getPackagingType()))) {
                    palletExchange = perPallet * safeInt(row.getPalletCount());
                }
            }

            ExtraTermSpec gb = findTerm(mergedExtras, "Gitterboxtausch");
            if (gb != null && "GP".equalsIgnoreCase(safeString(row.getPackagingType()))) {
                double perBox = "%".equals(gb.unit()) ? (netPrice * gb.value() / 100.0) : gb.value();
                palletBoxExchange = perBox * safeInt(row.getPalletCount());
            }

            // Flag-driven extras (basic €/%)
            Map<String, Double> extrasMap = new LinkedHashMap<>();
            addFlagExtra(extrasMap, mergedExtras, row.getExpressNextDay(), "Express Next Day", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getExpress12(), "Express 12:00 Uhr", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getExpress10(), "Express 10:00 Uhr", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getExpress8(), "Express 08:00 Uhr", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getFixtermin(), "Fixtermin", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getEmailAvis(), "E-Mail Avis", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getPhoneAvis(), "Telefonisches Avis", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getBookingInAvis(), "Booking in Avis", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getDangerousGoodsSurcharge(), "Gefahrgutzuschlag", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getLongGoodsSurcharge(), "Langgutzuschlag", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getShortWeekSurcharge(), "Kurzwochenzuschlag", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getCarrierCertificate(), "Spediteurbescheinigung", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getB2cNationalSurcharge(), "B2C Zuschlag (national)", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getB2cInternationalSurcharge(), "B2C Zuschlag (international)", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getSecurityFee(), "Security Fee", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getInsurance(), "Versicherung", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getPortiPapiere(), "Porti/Papiere", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getCustom1(), "Eigene 1", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getCustom2(), "Eigene 2", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getCustom3(), "Eigene 3", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getCustom4(), "Eigene 4", netPrice);
            addFlagExtra(extrasMap, mergedExtras, row.getCustom5(), "Eigene 5", netPrice);

            // Totals
            row.setPrice(round2(netPrice));
            row.setToll(round2(totalToll));
            row.setTollPercent(round2(tollValue));
            row.setDiesel(round2(dieselEuro));
            row.setDieselPercent(round2(dieselPercent));

            double extrasSum = extrasMap.values().stream().mapToDouble(ShipmentSummaryService::round2).sum()
                    + round2(palletExchange) + round2(palletBoxExchange);

            double totalExtraCosts = round2(totalToll + dieselEuro + extrasSum);
            double totalRowPrice   = round2(netPrice + totalExtraCosts);

            row.setExtraCostsTotalPrice(totalExtraCosts);
            row.setTotalPrice(totalRowPrice);

            // DO NOT RESET MESSAGE IF ERROR EXISTS
            if (row.getErrorType() == null || row.getErrorType() == 0) {
                row.setMessage("");
            }

            log.debug("calculateRow: sid={} country={} net={} toll={} diesel={} extras={} total={}",
                    safeString(row.getShipmentId()), country, row.getPrice(), row.getToll(), row.getDiesel(),
                    row.getExtraCostsTotalPrice(), row.getTotalPrice());

            return row;
        } catch (Exception ex) {
            log.error("calculateRow: error -> {}", ex.getMessage(), ex);
            setError(row, "Berechnungsfehler", 99);
            return row;
        }
    }
    
    // ---------------------------------------------------------------------
    // Extra costs (merge & lookup) + Diesel Floater config/matrix
    // ---------------------------------------------------------------------

    public record ExtraTermSpec(String term, String unit, double value) { }

    public record DieselFloaterConfig(String source, List<Map<String, Double>> brackets) { }

    /** Merge Base (and Additional, if present) for country (with INT fallback). */
    private List<ExtraTermSpec> getMergedExtraCost(Map<String, Object> extraCostsCatalog, String country) {
        if (extraCostsCatalog == null || extraCostsCatalog.isEmpty()) return List.of();

        Map<String, Object> intNode     = getCountryNode(extraCostsCatalog, "INT");
        Map<String, Object> countryNode = getCountryNode(extraCostsCatalog, country);

        List<Map<String, Object>> intBase   = getList(intNode.get("Base"));
        List<Map<String, Object>> intAdd    = getList(intNode.get("Additional"));
        List<Map<String, Object>> cBase     = getList(countryNode.get("Base"));
        List<Map<String, Object>> cAdd      = getList(countryNode.get("Additional"));

        Map<String, ExtraTermSpec> byTerm = new LinkedHashMap<>();
        for (Map<String, Object> m : intBase) putTerm(byTerm, m);
        for (Map<String, Object> m : intAdd)  putTerm(byTerm, m);
        for (Map<String, Object> m : cBase)   putTerm(byTerm, m);
        for (Map<String, Object> m : cAdd)    putTerm(byTerm, m);

        return new ArrayList<>(byTerm.values());
    }

    /**
     * Extract a Term spec (term, unit, value) from your flexible extraCosts map:
     *  extraCosts.get(country).get("Base") -> List of items with {"Term","Value","Unit"}.
     */
    private ExtraTermSpec findExtraTerm(Map<String, Object> extraCosts, String country, String term) {
        if (extraCosts == null || term == null) return null;

        Map<String, Object> countryNode = getCountryNode(extraCosts, country);
        if (countryNode.isEmpty()) return null;

        List<Map<String, Object>> base = getList(countryNode.get("Base"));
        for (Map<String, Object> item : base) {
            String t = safeString(item.get("Term"));
            if (term.equals(t)) {
                double value = toDouble(item.get("Value"));
                String unit  = safeString(item.getOrDefault("Unit", "€"));
                return new ExtraTermSpec(term, unit, value);
            }
        }

        List<Map<String, Object>> additional = getList(countryNode.get("Additional"));
        for (Map<String, Object> item : additional) {
            String t = safeString(item.get("Term"));
            if (term.equals(t)) {
                double value = toDouble(item.get("Value"));
                String unit  = safeString(item.getOrDefault("Unit", "€"));
                return new ExtraTermSpec(term, unit, value);
            }
        }

        Map<String, Object> intNode = getCountryNode(extraCosts, "INT");
        List<Map<String, Object>> intBase = getList(intNode.get("Base"));
        for (Map<String, Object> item : intBase) {
            String t = safeString(item.get("Term"));
            if (term.equals(t)) {
                double value = toDouble(item.get("Value"));
                String unit  = safeString(item.getOrDefault("Unit", "€"));
                return new ExtraTermSpec(term, unit, value);
            }
        }

        return null;
    }

    /**
     * Read a numeric property from ShipmentItemDocument by TS property name.
     * Used during consolidation sums (SUM_PROPERTIES).
     */
    private static double getNumeric(ShipmentItemDocument row, String prop) {
        switch (prop) {
            case "PalletCount":
                return row.getPalletCount() == null ? 0.0 : row.getPalletCount();
            case "EffectiveWeight":
                return row.getEffectiveWeight() == null ? 0.0 : row.getEffectiveWeight();
            case "LoadingMeters":
                return row.getLoadingMeters() == null ? 0.0 : row.getLoadingMeters();
            case "WeightByLoadingMeters":
                return row.getWeightByLoadingMeters() == null ? 0.0 : row.getWeightByLoadingMeters();
            case "WeightByCubicMeters":
                return row.getWeightByCubicMeters() == null ? 0.0 : row.getWeightByCubicMeters();
            case "MinimumWeight":
                return row.getMinimumWeight() == null ? 0.0 : row.getMinimumWeight();
            case "ChargeableWeight":
                return row.getChargeableWeight() == null ? 0.0 : row.getChargeableWeight();
            case "Price":
                return row.getPrice() == null ? 0.0 : row.getPrice();
            default:
                return 0.0;
        }
    }

    private void putTerm(Map<String, ExtraTermSpec> byTerm, Map<String, Object> m) {
        String term = safeString(m.get("Term"));
        if (term.isBlank()) return;
        String unit = safeString(m.getOrDefault("Unit", "€"));
        double value = toDouble(m.get("Value"));
        byTerm.put(term, new ExtraTermSpec(term, unit, value));
    }

    private ExtraTermSpec findTerm(List<ExtraTermSpec> list, String term) {
        for (ExtraTermSpec s : list) if (term.equals(s.term())) return s;
        return null;
    }

    private void addFlagExtra(Map<String, Double> extras, List<ExtraTermSpec> list,
                              Boolean flag, String term, double netPrice) {
        if (Boolean.TRUE.equals(flag)) {
            ExtraTermSpec spec = findTerm(list, term);
            if (spec != null) {
                double v = "%".equals(spec.unit()) ? (netPrice * spec.value() / 100.0) : spec.value();
                if (v > 0) extras.put(term, round2(v));
            }
        }
    }

    /** Extract DieselFloater config directly from extraCosts raw map (country node). */
    private DieselFloaterConfig extractDieselFloaterConfig(Map<String, Object> extraCosts, String country) {
        Map<String, Object> countryNode = getCountryNode(extraCosts, country);
        if (countryNode.isEmpty()) return null;

        Object dfObj = countryNode.get("DieselFloater");
        if (dfObj instanceof Map<?, ?> df) {
            String source = safeString(df.get("DieselFloaterSource"));
            Object vals   = df.get("DieselFloaterValues");
            List<Map<String, Double>> brackets = normalizeDieselBrackets(vals);
            if (!source.isBlank() && !brackets.isEmpty()) {
                return new DieselFloaterConfig(source, brackets);
            }
        }

        List<Map<String, Object>> base = getList(countryNode.get("Base"));
        for (Map<String, Object> item : base) {
            if ("Dieselzuschlag".equals(safeString(item.get("Term")))) {
                Object innerDfObj = item.get("DieselFloater");
                if (innerDfObj instanceof Map<?, ?> innerDf) {
                    String source = safeString(innerDf.get("DieselFloaterSource"));
                    Object vals   = innerDf.get("DieselFloaterValues");
                    List<Map<String, Double>> brackets = normalizeDieselBrackets(vals);
                    if (!source.isBlank() && !brackets.isEmpty()) {
                        return new DieselFloaterConfig(source, brackets);
                    }
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Double>> normalizeDieselBrackets(Object vals) {
        if (!(vals instanceof List<?> list)) return List.of();
        List<Map<String, Double>> brackets = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof Map<?, ?> m) {
                Map<String, Double> normalized = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    String k = safeString(e.getKey());
                    double v = toDouble(e.getValue());
                    normalized.put(k, v);
                }
                brackets.add(normalized);
            }
        }
        return brackets;
    }

    /** Helper to accumulate numeric values during consolidation. */
    private static void addNumeric(Map<String, Double> sums, String prop, double delta) {
        sums.put(prop, sums.getOrDefault(prop, 0.0) + delta);
    }

    /**
     * Diesel floater matrix lookup:
     *  - years → List(months) → Map(source→value)
     *  - uses previous month of shipmentDate (Jan → Dec previous year)
     */
    @SuppressWarnings("unchecked")
    private double getDieselFloaterMatrixValue(Map<String, Object> dieselFloaterMatrix,
                                               String shipmentDate, String source) {
        if (dieselFloaterMatrix == null || dieselFloaterMatrix.isEmpty()) return 0.0;
        if (isBlank(shipmentDate) || isBlank(source)) return 0.0;

        LocalDate date;
        try {
            String[] parts = shipmentDate.split("-");
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int d = Math.min(parts.length > 2 ? Integer.parseInt(parts[2]) : 1, 28);
            date = LocalDate.of(y, m, d);
        } catch (Exception e) {
            log.warn("getDieselFloaterMatrixValue: invalid shipmentDate '{}'", shipmentDate);
            return 0.0;
        }

        LocalDate prev = date.minusMonths(1);
        String yearKey = String.valueOf(prev.getYear());
        int monthIndex = prev.getMonthValue() - 1; // 0..11

        Object yearDataObj = dieselFloaterMatrix.get(yearKey);
        if (!(yearDataObj instanceof List<?> yearData) || yearData.isEmpty()) return 0.0;
        if (monthIndex < 0 || monthIndex >= yearData.size()) return 0.0;

        Object monthEntryObj = yearData.get(monthIndex);
        if (!(monthEntryObj instanceof Map<?, ?> monthEntry)) return 0.0;

        double value = toDouble(monthEntry.get(source));
        log.debug("DieselFloaterMatrix: year={} monthIdx={} source={} value={}", yearKey, monthIndex, source, value);
        return value;
    }

    /** Map the matrix value to a percent using brackets. */
    private double getDieselFloaterPercent(List<Map<String, Double>> brackets, double matrixValue) {
        if (brackets == null || brackets.isEmpty()) return 0.0;
        double matchedPercent = 0.0;
        for (Map<String, Double> b : brackets) {
            for (Map.Entry<String, Double> e : b.entrySet()) {
                double threshold = toDouble(e.getKey());
                double percent   = e.getValue() != null ? e.getValue() : 0.0;
                if (matrixValue <= threshold && matchedPercent == 0.0) {
                    matchedPercent = percent;
                    break;
                }
            }
            if (matchedPercent != 0.0) break;
        }
        return matchedPercent;
    }

    // ---------------------------------------------------------------------
    // Utility helpers
    // ---------------------------------------------------------------------

    private static double opt(Double d) { return d == null ? 0.0 : d; }
    private static int    optInt(Integer i) { return i == null ? 0 : i; }
    private static double safeDouble(Double d) { return d == null ? 0.0 : d; }

    private void setError(ShipmentItemDocument row, String msg, int code) {
        row.setMessage(msg);
        row.setErrorType(code);
        row.setPrice(0.0);
        row.setToll(0.0);
        row.setDiesel(0.0);
        row.setExtraCostsTotalPrice(0.0);
        row.setTotalPrice(0.0);
    }

    // ---------- Safe coercion helpers ----------

    /** Coerce an Object to int safely. Supports Number and String; otherwise 0. */
    private static int safeInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (Exception ignore) { return 0; }
        }
        return 0;
    }

    /** Coerce an Object to double safely. Supports Number and String; otherwise 0.0. */
    private static double safeDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) {
            try { return Double.parseDouble(s.trim()); } catch (Exception ignore) { return 0.0; }
        }
        return 0.0;
    }

    /** Existing overload for Integer (keep this if already present). */
    private static int safeInt(Integer i) { return i == null ? 0 : i; }

    // ---------- Collections / map readers ----------

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Object o) {
        if (o instanceof List<?>) {
            List<?> raw = (List<?>) o;
            if (!raw.isEmpty() && !(raw.get(0) instanceof Map)) {
                return List.of();
            }
            return (List<Map<String, Object>>) raw;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCountryNode(Map<String, Object> dict, String country) {
        if (dict == null || dict.isEmpty()) return new HashMap<>();
        Object node = dict.get(country);
        if (node instanceof Map) return (Map<String, Object>) node;
        Object intl = dict.get("INT");
        return (intl instanceof Map) ? (Map<String, Object>) intl : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getNode(Map<String, Object> parent, String key) {
        if (parent == null) return new HashMap<>();
        Object node = parent.get(key);
        return (node instanceof Map) ? (Map<String, Object>) node : new HashMap<>();
    }

    private Map<String, Object> firstNonNull(Map<String, Object> a, Map<String, Object> b) {
        return (a != null && !a.isEmpty()) ? a : (b != null ? b : new HashMap<>());
    }

    // ---------- String / number utils ----------

    private static String safeString(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static boolean isZero(Double d) { return d == null || d.doubleValue() == 0.0; }

    private static double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception ignore) { return 0.0; }
    }

    private static double toFixed(double value, int digits) {
        double m = Math.pow(10.0, digits);
        return Math.round(value * m) / m;
    }

    private static double round2(double v) { return toFixed(v, 2); }

    /**
     * Resolve stack factor consistently with TS logic.
     */
    private int resolveRowStackFactor(Integer stackFactor, boolean stackable) {
        if (!stackable) return 1;
        int sf = (stackFactor == null) ? 1 : stackFactor;
        if (sf < 1) return 1;
        return sf;
    }

    private double computeMinimumWeight(Map<String, Object> minWeight,
                                        String packagingType,
                                        int palletCount) {
        if (minWeight == null || packagingType == null) return 0.0;

        Map<String, Object> base = getNode(minWeight, "Base");
        Map<String, Object> baseEntry = getNode(base, packagingType);
        double baseWeight = toDouble(baseEntry.get("Weight"));

        Object additionalObj = minWeight.get("Additional");
        double additionalWeight = 0.0;
        if (additionalObj instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> m) {
                    String shorthand = safeString(m.get("Shorthand"));
                    if (packagingType.equals(shorthand)) {
                        additionalWeight = toDouble(m.get("Weight"));
                        break;
                    }
                }
            }
        }

        double perUnit = baseWeight > 0 ? baseWeight : additionalWeight;
        return perUnit > 0 ? (perUnit * palletCount) : 0.0;
    }

    private double normalizeChargeableWeight(String rateType, double chargeable, ShipmentItemDocument row) {
        switch (safeString(rateType)) {
            case "Gewicht 100Kg":
                return chargeable / 100.0;
            case "Gewicht 100Kg aufgerundet":
                return Math.ceil(chargeable / 100.0);
            case "Gewicht 10Kg aufgerundet":
                return (Math.ceil(chargeable / 10.0) * 10.0) / 100.0;
            case "Kilogramm":
                return chargeable;
            case "Paletten":
            case "Pakettarif":
                return safeInt(row.getPalletCount());
            case "Lademeter":
                return safeDouble(row.getLoadingMeters());
            default:
                return chargeable;
        }
    }

    private String findMatchedWeightKey(Set<String> keys, double normalizedChargeable) {
        List<Double> doubles = keys.stream().map(k -> {
            try { return Double.parseDouble(k); } catch (Exception e) { return Double.POSITIVE_INFINITY; }
        }).sorted().collect(Collectors.toList());
        for (Double d : doubles) {
            if (normalizedChargeable <= d) return d % 1 == 0 ? String.valueOf(d.intValue()) : String.valueOf(d);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getZipCodes(Map<String, Object> countryRate) {
        Object zips = countryRate.get("ZipCodes");
        if (zips instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private int resolveZip(ShipmentItemDocument row) {
        String zipStr = (row.getProjectType() != null && row.getProjectType() == 1)
                ? safeString(row.getZipCodeShipper())
                : safeString(row.getZipCodeConsignee());
        if (zipStr.isBlank()) zipStr = "00000";
        try { return Integer.parseInt(zipStr.replaceAll("\\D", "")); } catch (Exception e) { return 0; }
    }

    private String findZipZoneId(List<Map<String, Object>> zipCodesList, int zip) {
        List<Map<String, Object>> matches = zipCodesList.stream()
                .filter(z -> zipInCodes(zip, safeString(z.get("Codes"))))
                .sorted(Comparator.comparing(m -> safeString(m.get("Zone"))))
                .collect(Collectors.toList());
        Collections.reverse(matches);
        return matches.isEmpty() ? null : safeString(matches.get(0).get("Id"));
    }

    private Map<String, Object> findZipRangeById(List<Map<String, Object>> zipCodesList, String id) {
        for (Map<String, Object> z : zipCodesList) {
            if (id.equals(safeString(z.get("Id")))) return z;
        }
        return new HashMap<>();
    }

    private boolean zipInCodes(int zip, String codes) {
        String normalized = codes.replaceAll("[ /]", "");
        String[] parts = normalized.split(",");
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (part.contains("-")) {
                String[] range = part.split("-");
                int start = toZip(range[0], true);
                int end   = toZip(range[1], false);
                if (zip == end || (zip >= start && zip <= end)) return true;
            } else {
                int start = toZip(part, true);
                int end   = toZip(part, false);
                if (zip == end || (zip >= start && zip <= end)) return true;
            }
        }
        return false;
    }

    private int toZip(String s, boolean start) {
        String padded = s.trim();
        while (padded.length() < 5) padded += (start ? "0" : "9");
        try { return Integer.parseInt(padded); } catch (Exception e) { return 0; }
    }

    @SuppressWarnings("unchecked")
    private double getPriceForZone(Map<String, Object> weightRow, String zoneId, String rateType, double normalizedChargeable) {
        Map<String, Object> prices = getNode(weightRow, "Prices");
        double unitPrice = toDouble(prices.get(zoneId));
        // return normalizedChargeable * unitPrice;
        return unitPrice;
    }

    private double applyMinMaxIfAny(double price, Map<String, Object> zipRange, String rateType) {
        if (!ALLOWED_MIN_MAX_TARIFFS.contains(safeString(rateType))) return price;
        double min = toDouble(zipRange.get("MinPrice"));
        double max = toDouble(zipRange.get("MaxPrice"));
        if (min > 0 && price < min) return min;
        if (max > 0 && price > max) return max;
        return price;
    }

    // ---------------------------------------------------------------------
    // DTOs (kept inside the service for single-class requirement)
    // ---------------------------------------------------------------------

    @Data
    public static class RowSummedTotal {
        private int    shipmentDataLength;
        private double price;                 // net price
        private double tollPrice;             // € or % of net
        private double dieselPrice;           // sum per-row diesel
        private double totalExtraCostsPrice;  // toll + diesel + consolidated extras
        private double totalPrice;            // net + extras
    }

    @Data
    @AllArgsConstructor
    public static class ShipmentTotalSummary {
        private Map<String, RowSummedTotal> countriesRowTotal;
        private double totalShipmentPrice;
    }

    @Data
    public static class SummaryInitResult {
        private ShipmentTotalSummary                    shipmentTotalSummary;
        private Map<String, List<ShipmentItemDocument>> consolidatedShipmentData;
        private Map<String, Object>                     fetchedShipperExtraCosts;
        private Map<String, Object>                     dieselFloaterMatrix;
    }
}
