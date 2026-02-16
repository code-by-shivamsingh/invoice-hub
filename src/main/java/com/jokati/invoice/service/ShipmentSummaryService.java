package com.jokati.invoice.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

@Service
@Slf4j
public class ShipmentSummaryService {

	private final ProjectShipmentRepository projectShipmentRepository;

	private final ShipperFreightCalculationBasisService freightService;
	private final ShipperRatesService ratesService;
	private final ShipperExtraCostsService extraCostsService;
	private final DieselFloaterService dieselService;

	public ShipmentSummaryService(ProjectShipmentRepository projectShipmentRepository,
			ShipperFreightCalculationBasisService freightService, ShipperRatesService ratesService,
			ShipperExtraCostsService extraCostsService, DieselFloaterService dieselService) {
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
			Map.entry("ExpressNextDay", "Express Next Day"), Map.entry("Express12", "Express 12:00 Uhr"),
			Map.entry("Express10", "Express 10:00 Uhr"), Map.entry("Express8", "Express 08:00 Uhr"),
			Map.entry("TailLiftSurcharge", "Hebebühnenzuschlag"), Map.entry("Fixtermin", "Fixtermin"),
			Map.entry("EmailAvis", "E-Mail Avis"), Map.entry("PhoneAvis", "Telefonisches Avis"),
			Map.entry("BookingInAvis", "Booking in Avis"), Map.entry("DangerousGoodsSurcharge", "Gefahrgutzuschlag"),
			Map.entry("LongGoodsSurcharge", "Langgutzuschlag"), Map.entry("ShortWeekSurcharge", "Kurzwochenzuschlag"),
			Map.entry("PalletExchange", "Palettentausch"), Map.entry("PalletBoxExchange", "Gitterboxtausch"),
			Map.entry("CarrierCertificate", "Spediteurbescheinigung"),
			Map.entry("B2CNationalSurcharge", "B2C Zuschlag (national)"),
			Map.entry("B2CInternationalSurcharge", "B2C Zuschlag (international)"),
			Map.entry("SecurityFee", "Security Fee"), Map.entry("Insurance", "Versicherung"),
			Map.entry("PortiPapiere", "Porti/Papiere"), Map.entry("Custom1", "Eigene 1"),
			Map.entry("Custom2", "Eigene 2"), Map.entry("Custom3", "Eigene 3"), Map.entry("Custom4", "Eigene 4"),
			Map.entry("Custom5", "Eigene 5"));

	/** Keys whose numeric values are summed in consolidation. */
	private static final List<String> SUM_PROPERTIES = List.of("PalletCount", "EffectiveWeight", "LoadingMeters",
			"WeightByLoadingMeters", "WeightByCubicMeters", "MinimumWeight", "ChargeableWeight", "Price");

	/** Tariffs that allow Min/Max application (TS parity). */
	private static final Set<String> ALLOWED_MIN_MAX_TARIFFS = Set.of("Gewicht 100Kg", "Gewicht 100Kg aufgerundet",
			"Gewicht 10Kg aufgerundet", "Kilogramm", "Kilometer");

	// ---------------------------------------------------------------------
	// Public API
	// ---------------------------------------------------------------------

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
			List<ShipmentItemDocument> rawRows = Optional.ofNullable(project.getShipmentData())
					.orElseGet(ArrayList::new);

			Map<String, Object> freightBasis = fetchFreightBasis(projectIdStr);
			Map<String, Object> rates = fetchRates(projectIdStr);
			Map<String, Object> extraCosts = fetchExtraCosts(projectIdStr);
			Map<String, Object> dieselFloaterMatrix = fetchDieselFloaterMatrix();

			List<ShipmentItemDocument> calculatedRows = rawRows.stream()
					.map(r -> calculateRow(r, freightBasis, rates, extraCosts, dieselFloaterMatrix))
					.collect(Collectors.toList());

			Map<String, List<ShipmentItemDocument>> preparedByCountry = calculatedRows.stream()
					.collect(Collectors.groupingBy(row -> Optional.ofNullable(row.getCountry()).orElse("INT"),
							LinkedHashMap::new, Collectors.toList()));

			Map<String, List<ShipmentItemDocument>> consolidatedByCountry = createSortedConsolidatedShipmentData(
					preparedByCountry, freightBasis, rates, extraCosts, dieselFloaterMatrix);

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

			List<ShipmentItemDocument> rawRows = Optional.ofNullable(project.getShipmentData())
					.orElseGet(ArrayList::new);

			Map<String, Object> freightBasis = fetchFreightBasis(projectId);
			Map<String, Object> rates = fetchRates(projectId);
			Map<String, Object> extraCosts = fetchExtraCosts(projectId);
			Map<String, Object> dieselFloaterMatrix = fetchDieselFloaterMatrix();

			List<ShipmentItemDocument> calculatedRows = rawRows.stream()
					.map(r -> calculateRow(r, freightBasis, rates, extraCosts, dieselFloaterMatrix))
					.collect(Collectors.toList());

			List<ShipmentItemDocument> filteredRows = calculatedRows.stream()
					.filter(item -> item.getMessage() != null && !item.getMessage().trim().isEmpty())
					.collect(Collectors.toList());

			Map<String, List<ShipmentItemDocument>> preparedByCountry = filteredRows.stream()
					.collect(Collectors.groupingBy(row -> Optional.ofNullable(row.getCountry()).orElse("INT"),
							LinkedHashMap::new, Collectors.toList()));

			Map<String, List<ShipmentItemDocument>> consolidatedByCountry = createSortedConsolidatedShipmentData(
					preparedByCountry, freightBasis, rates, extraCosts, dieselFloaterMatrix);

			Map<String, RowSummedTotal> countriesTotals = createCountryRowTotal(consolidatedByCountry, extraCosts);

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

	public SummaryInitResult getSummaryByShipmentId(ObjectId projectId, String shipmentId) {
		try {
			final String projectIdStr = projectId.toHexString();

			final String sid = normalizeShipmentId(shipmentId);

			log.info("getSummaryByShipmentId: projectId={} shipmentId(raw)='{}' normalized='{}'", projectIdStr,
					shipmentId, sid);

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

			List<ShipmentItemDocument> rawRows = Optional.ofNullable(project.getShipmentData())
					.orElseGet(ArrayList::new);

			List<ShipmentItemDocument> filteredRows = rawRows.stream()
					.filter(r -> sid.equals(normalizeShipmentId(r.getShipmentId()))).collect(Collectors.toList());

			if (filteredRows.isEmpty()) {
				log.warn("getSummaryByShipmentId: no rows for sid={}", sid);
				return null;
			}

			Map<String, Object> freightBasis = fetchFreightBasis(projectIdStr);
			Map<String, Object> rates = fetchRates(projectIdStr);
			Map<String, Object> extraCosts = fetchExtraCosts(projectIdStr);
			Map<String, Object> dieselFloaterMatrix = fetchDieselFloaterMatrix();

			List<ShipmentItemDocument> calculatedRows = filteredRows.stream()
					.map(r -> calculateRow(r, freightBasis, rates, extraCosts, dieselFloaterMatrix))
					.collect(Collectors.toList());

			Map<String, List<ShipmentItemDocument>> preparedByCountry = calculatedRows.stream()
					.collect(Collectors.groupingBy(row -> Optional.ofNullable(row.getCountry()).orElse("INT"),
							LinkedHashMap::new, Collectors.toList()));

			Map<String, List<ShipmentItemDocument>> consolidatedByCountry = createSortedConsolidatedShipmentData(
					preparedByCountry, freightBasis, rates, extraCosts, dieselFloaterMatrix);

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
	// Helpers
	// ---------------------------------------------------------------------

	private static String normalizeShipmentId(Object shipmentId) {
		if (shipmentId == null)
			return null;
		String s = String.valueOf(shipmentId);
		try {
			s = s.strip();
		} catch (Throwable t) {
			s = s.trim();
		}
		s = s.replace("\u00A0", "");
		s = s.replaceAll("\\s+", "");
		return s.isEmpty() ? null : s;
	}

	private Map<String, Object> fetchFreightBasis(String projectIdHex) {
		try {
			Optional<ShipperFreightCalculationBasis> opt = freightService.findByProjectId(projectIdHex);
			Map<String, Object> countries = opt.map(ShipperFreightCalculationBasis::getCountries)
					.orElseGet(HashMap::new);
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

	/**
	 * Diesel Floater matrix: years → List(months) → Map(source→value). Picks latest
	 * document by ObjectId timestamp.
	 */
	private Map<String, Object> fetchDieselFloaterMatrix() {
		try {
			List<DieselFloater> all = dieselService.findAll();
			if (all == null || all.isEmpty()) {
				log.warn("fetchDieselFloaterMatrix: none found");
				return new HashMap<>();
			}
			DieselFloater latest = all.stream().max(Comparator.comparingInt(df -> safeObjectIdTimestamp(df.getId())))
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
	// Sorting & Consolidation (TS parity)
	// ---------------------------------------------------------------------

	public Map<String, List<ShipmentItemDocument>> createSortedConsolidatedShipmentData(
			Map<String, List<ShipmentItemDocument>> groupedByCountry, Map<String, Object> freightBasis, // NEW: for
																										// IsConsolidated
																										// gating
			Map<String, Object> rates, Map<String, Object> extraCosts, Map<String, Object> dieselFloaterMatrix) {
		Map<String, List<ShipmentItemDocument>> result = new LinkedHashMap<>();
		if (groupedByCountry == null || groupedByCountry.isEmpty())
			return result;

		for (Map.Entry<String, List<ShipmentItemDocument>> e : groupedByCountry.entrySet()) {
			String country = e.getKey();
			List<ShipmentItemDocument> rows = e.getValue();

			List<ShipmentItemDocument> sorted = sortByShipmentId(rows);
			boolean consolidate = isConsolidatedEnabled(freightBasis, country);
			List<ShipmentItemDocument> consolidated = consolidate
					? createConsolidatedRows(sorted, country, rates, extraCosts, dieselFloaterMatrix)
					: sorted;

			result.put(country, consolidated);
		}

		return result;
	}

	private boolean isConsolidatedEnabled(Map<String, Object> freightBasis, String country) {
		if (freightBasis == null)
			return false;
		Map<String, Object> intNode = getCountryNode(freightBasis, "INT");
		Map<String, Object> cNode = getCountryNode(freightBasis, country);
		boolean intFlag = Boolean.TRUE.equals(intNode.get("IsConsolidated"));
		boolean cFlag = Boolean.TRUE.equals(cNode.get("IsConsolidated"));
		return intFlag || cFlag;
	}

	private List<ShipmentItemDocument> sortByShipmentId(List<ShipmentItemDocument> shipmentData) {
		if (shipmentData == null)
			return Collections.emptyList();

		return shipmentData.stream().sorted((a, b) -> {
			String aId = Optional.ofNullable(a.getShipmentId()).orElse("");
			String bId = Optional.ofNullable(b.getShipmentId()).orElse("");

			boolean aPresent = !aId.isBlank();
			boolean bPresent = !bId.isBlank();
			if (!aPresent && !bPresent)
				return 0;
			if (!aPresent)
				return 1;
			if (!bPresent)
				return -1;

			boolean aNumeric = aId.matches("\\d+");
			boolean bNumeric = bId.matches("\\d+");

			if (aNumeric && bNumeric) {
				return Long.compare(Long.parseLong(aId), Long.parseLong(bId));
			}
			if (aNumeric)
				return -1;
			if (bNumeric)
				return 1;
			return aId.compareToIgnoreCase(bId);
		}).collect(Collectors.toList());
	}

	private List<ShipmentItemDocument> createConsolidatedRows(List<ShipmentItemDocument> rowsByCountry, String country,
			Map<String, Object> rates, Map<String, Object> extraCosts, Map<String, Object> dieselFloaterMatrix) {
		log.info("dieselFloaterMatrix is {}", dieselFloaterMatrix.isEmpty());
		Map<String, Map<String, Double>> sumsPerShipment = new LinkedHashMap<>();
		Map<String, Integer> lastIndexPerShipment = new LinkedHashMap<>();
		List<ShipmentItemDocument> out = new ArrayList<>();

		List<String> consolidatedShipmentIds = getConsolidatedShipmentIds(rowsByCountry);

		// consolidated extras rules (TS parity)
		final Set<String> extraCostsToCalculateOnce = Set.of("ExpressNextDay", "Express12", "Express10", "Express8",
				"Fixtermin", "EmailAvis", "PhoneAvis", "BookingInAvis", "DangerousGoodsSurcharge", "LongGoodsSurcharge",
				"ShortWeekSurcharge", "CarrierCertificate", "B2CNationalSurcharge", "B2CInternationalSurcharge",
				"SecurityFee", "Insurance", "PortiPapiere");
		final Set<String> extraCostsToCalculateInPercent = Set.of("DangerousGoodsSurcharge", "ShortWeekSurcharge",
				"B2CNationalSurcharge", "B2CInternationalSurcharge");
		final Set<String> extraCostsToCalculatePerPallet = Set.of("PalletExchange", "PalletBoxExchange");

		for (int idx = 0; idx < rowsByCountry.size(); idx++) {
			ShipmentItemDocument row = rowsByCountry.get(idx);
			String sid = row.getShipmentId();

			if (sid != null && !sid.isBlank()) {
				Map<String, Double> sums = sumsPerShipment.computeIfAbsent(sid, k -> new HashMap<>());
				SUM_PROPERTIES.forEach(prop -> addNumeric(sums, prop, getNumeric(row, prop)));

				boolean isGroup = consolidatedShipmentIds.contains(sid);
				row.setIsConsolidated(isGroup);
				if (isGroup)
					lastIndexPerShipment.put(sid, idx);
			}

			out.add(row);
		}

		for (int i = 0; i < consolidatedShipmentIds.size(); i++) {

			String sid = consolidatedShipmentIds.get(i);
			log.info("consolidated shipment id {}", sid);
			int lastIndex = lastIndexPerShipment.getOrDefault(sid, -1);
			if (lastIndex < 0)
				continue;

			List<ShipmentItemDocument> groupRows = out.stream().filter(r -> sid.equals(r.getShipmentId()))
					.collect(Collectors.toList());

			double maxLength = groupRows.stream().mapToDouble(r -> opt(r.getLength())).max().orElse(0);
			boolean hasFP = groupRows.stream().anyMatch(r -> "FP".equalsIgnoreCase(r.getPackagingType()));
			int fpPalletCount = groupRows.stream().filter(r -> "FP".equalsIgnoreCase(r.getPackagingType()))
					.mapToInt(r -> optInt(r.getPalletCount())).sum();

			// consolidated extras map per TS rules
			Map<String, Double> consolidatedExtraCosts = new LinkedHashMap<>();
			// once/max (with % special-case already applied per row)
			for (String key : extraCostsToCalculateOnce) {
				double acc = 0.0;
				for (ShipmentItemDocument r : groupRows) {
					Map<String, Double> rowExtras = r.getExtraCosts() != null ? r.getExtraCosts() : Map.of();
					Double v = rowExtras.get(key);
					if (v != null)
						acc = Math.max(acc, v);
				}
				if (acc > 0)
					consolidatedExtraCosts.put(key, round2(acc));
			}
			// per-pallet SUM
			for (String key : extraCostsToCalculatePerPallet) {
				double sum = 0.0;
				for (ShipmentItemDocument r : groupRows) {
					Map<String, Double> rowExtras = r.getExtraCosts() != null ? r.getExtraCosts() : Map.of();
					Double v = rowExtras.get(key);
					if (v != null)
						sum += v;
				}
				if (sum > 0)
					consolidatedExtraCosts.put(key, round2(sum));
			}
			Map<String, Boolean> consolidatedExtraFlags = consolidatedExtraCosts.keySet().stream()
					.collect(Collectors.toMap(k -> k, k -> Boolean.TRUE, (a, b) -> a, LinkedHashMap::new));

			int insertIndex = lastIndex + 1 + i;
			while (insertIndex < out.size() && sid.equals(out.get(insertIndex).getShipmentId())) {
				insertIndex++;
			}

			ShipmentItemDocument base = rowsByCountry.get(lastIndex);
			Map<String, Double> sums = sumsPerShipment.getOrDefault(sid, Collections.emptyMap());

			ShipmentItemDocument sumRow = ShipmentItemDocument.builder().idUpper(null)
					.idLower(UUID.randomUUID().toString()).shipmentId(sid).shipmentDate(base.getShipmentDate())
					.zipCodeShipper(base.getZipCodeShipper()).zipCodeConsignee(base.getZipCodeConsignee())
					.city(base.getCity()).country(base.getCountry()).kilometers(base.getKilometers()).length(maxLength)
					.wide(0.0).height(0.0).loadingMeters(sums.getOrDefault("LoadingMeters", 0.0)).cubicMeters(0.0)
					.palletCount(sums.getOrDefault("PalletCount", 0.0) == 0.0 ? 0 : sums.get("PalletCount").intValue())
					.packagingType("CC").effectiveWeight(sums.getOrDefault("EffectiveWeight", 0.0))
					.chargeableWeight(sums.getOrDefault("ChargeableWeight", 0.0))
					.minimumWeight(sums.getOrDefault("MinimumWeight", 0.0))
					.weightByCubicMeters(sums.getOrDefault("WeightByCubicMeters", 0.0))
					.weightByLoadingMeters(sums.getOrDefault("WeightByLoadingMeters", 0.0)).stackable(Boolean.FALSE)
					.stackFactor(1).stackId("").stackFootprintLoadingMeters(0.0).message("").errorType(0)
					.price(sums.getOrDefault("Price", 0.0)).totalPrice(0.0).extraCostsTotalPrice(0.0).toll(0.0)
					.tollPercent(0.0).diesel(0.0).dieselPercent(0.0).isConsolidated(Boolean.FALSE)
					.isConsolidatedSum(Boolean.TRUE).hasPackagingType(hasFP).hasFP(hasFP).fpPalletCount(fpPalletCount)
					.projectType(base.getProjectType()).undefined(base.getUndefined())
					// Pre-seed flags true on sum row (TS parity)
					.expressNextDay(consolidatedExtraFlags.getOrDefault("ExpressNextDay", false))
					.express12(consolidatedExtraFlags.getOrDefault("Express12", false))
					.express10(consolidatedExtraFlags.getOrDefault("Express10", false))
					.express8(consolidatedExtraFlags.getOrDefault("Express8", false))
					.tailLiftSurcharge(consolidatedExtraFlags.getOrDefault("TailLiftSurcharge", false))
					.fixtermin(consolidatedExtraFlags.getOrDefault("Fixtermin", false))
					.emailAvis(consolidatedExtraFlags.getOrDefault("EmailAvis", false))
					.phoneAvis(consolidatedExtraFlags.getOrDefault("PhoneAvis", false))
					.bookingInAvis(consolidatedExtraFlags.getOrDefault("BookingInAvis", false))
					.dangerousGoodsSurcharge(consolidatedExtraFlags.getOrDefault("DangerousGoodsSurcharge", false))
					.longGoodsSurcharge(consolidatedExtraFlags.getOrDefault("LongGoodsSurcharge", true))
					.shortWeekSurcharge(consolidatedExtraFlags.getOrDefault("ShortWeekSurcharge", false))
					.palletExchange(consolidatedExtraFlags.getOrDefault("PalletExchange", false))
					.palletBoxExchange(consolidatedExtraFlags.getOrDefault("PalletBoxExchange", false))
					.carrierCertificate(consolidatedExtraFlags.getOrDefault("CarrierCertificate", false))
					.b2cNationalSurcharge(consolidatedExtraFlags.getOrDefault("B2CNationalSurcharge", false))
					.b2cInternationalSurcharge(consolidatedExtraFlags.getOrDefault("B2CInternationalSurcharge", false))
					.securityFee(consolidatedExtraFlags.getOrDefault("SecurityFee", false))
					.insurance(consolidatedExtraFlags.getOrDefault("Insurance", false))
					.portiPapiere(consolidatedExtraFlags.getOrDefault("PortiPapiere", false))
					.custom1(consolidatedExtraFlags.getOrDefault("Custom1", false))
					.custom2(consolidatedExtraFlags.getOrDefault("Custom2", false))
					.custom3(consolidatedExtraFlags.getOrDefault("Custom3", false))
					.custom4(consolidatedExtraFlags.getOrDefault("Custom4", false))
					.custom5(consolidatedExtraFlags.getOrDefault("Custom5", false)).build();

			if (country.isBlank())
				country = "INT";
			sumRow = calculateRow(sumRow, Map.of(base.getCountry(), Map.of("IsConsolidated", true)), rates,
					Map.of(country, extraCosts.get(country)), dieselFloaterMatrix);

			// Override per TS consolidated extras logic
			if (sumRow.getExtraCosts() == null) {
				sumRow.setExtraCosts(new LinkedHashMap<>());
			}
			sumRow.getExtraCosts().putAll(consolidatedExtraCosts);
			double toll = opt(sumRow.getToll());
			double diesel = opt(sumRow.getDiesel());
			double extras = sumRow.getExtraCosts().values().stream().mapToDouble(ShipmentSummaryService::round2).sum();
			double totalExtras = round2(toll + diesel + extras);
			sumRow.setExtraCostsTotalPrice(totalExtras);
			sumRow.setTotalPrice(round2(opt(sumRow.getPrice()) + totalExtras));

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
			if (sid != null && !sid.isBlank())
				counts.put(sid, counts.getOrDefault(sid, 0) + 1);
		});
		return counts.entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	// ---------------------------------------------------------------------
	// Country totals & overall total (TS parity)
	// ---------------------------------------------------------------------

	public Map<String, RowSummedTotal> createCountryRowTotal(Map<String, List<ShipmentItemDocument>> groupedByCountry,
			Map<String, Object> extraCostsCatalog) {
		Map<String, RowSummedTotal> totals = new LinkedHashMap<>();
		if (groupedByCountry == null || groupedByCountry.isEmpty())
			return totals;

		for (Map.Entry<String, List<ShipmentItemDocument>> e : groupedByCountry.entrySet()) {
			String country = e.getKey();
			List<ShipmentItemDocument> rows = e.getValue();

			List<ShipmentItemDocument> baseRows = rows.stream().filter(r -> !Boolean.TRUE.equals(r.getIsConsolidated()))
					.collect(Collectors.toList());
			List<ShipmentItemDocument> sumRows = rows.stream()
					.filter(r -> Boolean.TRUE.equals(r.getIsConsolidatedSum())).collect(Collectors.toList());

			boolean consolidated = !sumRows.isEmpty();
			int shipmentDataLength = consolidated ? (int) rows.stream().filter(
					r -> Boolean.TRUE.equals(r.getIsConsolidatedSum()) || !Boolean.TRUE.equals(r.getIsConsolidated()))
					.count() : rows.size();

			double netPrice = baseRows.stream().mapToDouble(r -> opt(r.getPrice())).sum();

			RowSummedTotal bucket = new RowSummedTotal();
			bucket.setShipmentDataLength(shipmentDataLength);
			bucket.setPrice(round2(netPrice));

			// Toll unit from extra costs (Maut), respect Included flag
			boolean tollIncluded = isTollIncluded(extraCostsCatalog, country);
			ExtraTermSpec maut = findExtraTerm(extraCostsCatalog, country, "Maut");
			String tollUnit = tollIncluded ? "" : (maut == null ? "€" : Optional.ofNullable(maut.unit()).orElse("€"));

			if ("€".equals(tollUnit)) {
				double toll = baseRows.stream().mapToDouble(r -> opt(r.getToll())).sum();
				bucket.setTollPrice(round2(toll));
			} else if ("%".equals(tollUnit)) {
				double percent = maut.value() / 100.0;
				bucket.setTollPrice(round2(netPrice * percent));
			} else {
				bucket.setTollPrice(0.0);
			}

			double diesel = baseRows.stream().mapToDouble(r -> opt(r.getDiesel())).sum();
			bucket.setDieselPrice(round2(diesel));

			// Per-term extras from base rows (TS parity)
			Map<String, Double> perTerm = new LinkedHashMap<>();
			for (ShipmentItemDocument r : baseRows) {
				Map<String, Double> m = r.getExtraCosts();
				if (m == null)
					continue;
				for (Map.Entry<String, Double> en : m.entrySet()) {
					perTerm.merge(en.getKey(), opt(en.getValue()), Double::sum);
				}
			}
			perTerm.merge("Toll", bucket.getTollPrice(), Double::sum);
			perTerm.merge("Diesel", bucket.getDieselPrice(), Double::sum);

			double extrasTotal = perTerm.values().stream().mapToDouble(ShipmentSummaryService::round2).sum();
			bucket.setAllExtraCosts(perTerm);
			bucket.setTotalExtraCostsPrice(round2(extrasTotal));
			bucket.setTotalPrice(round2(bucket.getPrice() + extrasTotal));

			totals.put(country, bucket);
		}

		return totals;
	}

	private double addTotalShipmentPrice(Map<String, RowSummedTotal> byCountry) {
		if (byCountry == null || byCountry.isEmpty())
			return 0.0;
		return round2(byCountry.values().stream().mapToDouble(RowSummedTotal::getTotalPrice).sum());
	}

	// ---------------------------------------------------------------------
	// Row calculation (TS parity + Diesel Floater)
	// ---------------------------------------------------------------------

	private ShipmentItemDocument calculateRow(ShipmentItemDocument row, Map<String, Object> freightBasis,
			Map<String, Object> rates, Map<String, Object> extraCosts, Map<String, Object> dieselFloaterMatrix) {
		try {
			log.info("dieselFloaterMatrix value under {}", dieselFloaterMatrix.isEmpty());
			bindRawExtraCosts(extraCosts,
					safeString(row.getCountry()).isBlank() ? "INT" : safeString(row.getCountry()));

			final boolean isSumRow = Boolean.TRUE.equals(row.getIsConsolidatedSum());

			// Country normalize
			String country = safeString(row.getCountry());
			if (country.isBlank())
				country = "INT";
			row.setCountry(country);

			// Stack/Loading meters
			boolean stackable = Boolean.TRUE.equals(row.getStackable());
			int stackFactor = resolveRowStackFactor(row.getStackFactor(), stackable);
			row.setStackable(stackable);
			row.setStackFactor(stackFactor);

			double loadingMeters = safeDouble(row.getLoadingMeters());
			if (loadingMeters <= 0) {
				loadingMeters = toFixed(
						((safeDouble(row.getLength()) / 100.0) * (safeDouble(row.getWide()) / 100.0) / 2.4)
								* safeInt(row.getPalletCount()),
						3);
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
			Map<String, Object> advancedOptions = firstNonNull(getNode(basisForCountry, "advancedOptions"),
					getNode(basisForCountry, "AdvancedOptions"));

			double kgThreshold = toDouble(advancedOptions.get("LoadingMetersKg"));
			double ldmThreshold = toDouble(advancedOptions.get("LoadingMetersLdm"));

			// Switch to LM calculation based on thresholds; for sum rows we won't derive
			// weightBy*
			double effForCheck = isSumRow
					? Math.max(safeDouble(row.getEffectiveWeight()), safeDouble(row.getChargeableWeight()))
					: safeDouble(row.getEffectiveWeight());
			boolean meetsKg = kgThreshold > 0 && effForCheck >= kgThreshold;
			boolean meetsLdm = ldmThreshold > 0 && loadingMeters >= ldmThreshold;
			int effectiveCalculationType = (calculationType == 0 || calculationType == 1) && (meetsKg || meetsLdm) ? 2
					: calculationType;

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
				minWeightValue = computeMinimumWeight(minWeight, safeString(row.getPackagingType()),
						safeInt(row.getPalletCount()));
			}
			row.setWeightByCubicMeters(weightByCubicMeters);
			row.setWeightByLoadingMeters(weightByLoadingMeters);
			row.setMinimumWeight(minWeightValue);

			double chargeable = (isSumRow && row.getChargeableWeight() != null && row.getChargeableWeight() > 0)
					? row.getChargeableWeight()
					: Math.max(Math.max(safeDouble(row.getEffectiveWeight()), weightByCubicMeters),
							Math.max(weightByLoadingMeters, minWeightValue));
			row.setChargeableWeight(chargeable);

			// Rates & zone
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

				List<Map<String, Object>> zipCodesList = getZipCodes(countryRate);
				String zoneId;
				Map<String, Object> matchedZoneEntry;
				if ("Kilometer".equalsIgnoreCase(rateType)) {
					double km = safeDouble(row.getKilometers());
					var kmMatch = findKilometerZone(zipCodesList, km);
					zoneId = kmMatch != null ? safeString(kmMatch.get("Id")) : null;
					matchedZoneEntry = kmMatch;
					if (zoneId == null) {
						setError(row, "Kein Preis für die entsprechende Kilometerangabe eingetragen.", 12);
						return row;
					}
				} else {
					var match = findBestZipZoneEntry(zipCodesList, row);
					zoneId = match != null ? safeString(match.get("Id")) : null;
					matchedZoneEntry = match;
					if (zoneId == null) {
						setError(row, "Kein Preis für die entsprechende Postleitzahl eingetragen.", 12);
						return row;
					}
				}

				double basePrice = computeRowPrice(weightRow, zoneId, rateType, normalizedChargeable);
				netPrice = applyMinMaxIfAny(basePrice, matchedZoneEntry, rateType);
			} else {
				netPrice = opt(row.getPrice());
			}

			// Extra costs & Diesel Floater
			List<ExtraTermSpec> mergedExtras = getMergedExtraCost(extraCosts, country);

			// Toll (Maut), respect Included flag
			boolean tollIncluded = isTollIncluded(extraCosts, country);
			ExtraTermSpec maut = findTerm(mergedExtras, "Maut");
			String tollUnit = tollIncluded ? "" : (maut == null ? "€" : maut.unit());
			double tollValue = tollIncluded ? 0.0 : (maut == null ? 0.0 : maut.value());
			double totalToll = "€".equals(tollUnit) ? (safeInt(row.getPalletCount()) * tollValue)
					: (netPrice * tollValue / 100.0);
			if (tollIncluded)
				totalToll = 0.0;

			// Diesel Floater config
			DieselFloaterConfig floaterCfg = extractDieselFloaterConfig(extraCosts, country);
			double dieselPercent = 0.0;
			double dieselEuro = 0.0;

			if (floaterCfg != null) {
				double matrixVal = getDieselFloaterMatrixValue(dieselFloaterMatrix, safeString(row.getShipmentDate()),
						floaterCfg.source());
				dieselPercent = getDieselFloaterPercent(floaterCfg.brackets(), matrixVal);
				dieselEuro = (netPrice / 100.0) * dieselPercent;
			} else {
				// fallback: plain Dieselzuschlag
				ExtraTermSpec dieselSpec = findTerm(mergedExtras, "Dieselzuschlag");
				if (dieselSpec != null) {
					if ("%".equals(dieselSpec.unit())) {
						dieselPercent = dieselSpec.value();
						dieselEuro = (netPrice / 100.0) * dieselPercent;
					} else {
						dieselEuro = dieselSpec.value();
					}
				}
			}

			// Palettentausch & Gitterboxtausch (integer pallets)
			double palletExchange = 0.0;
			double palletBoxExchange = 0.0;

			ExtraTermSpec pe = findTerm(mergedExtras, "Palettentausch");
			if (pe != null) {
				// Per-pallet charge: either % of net per pallet OR flat per pallet
				double perPallet = "%".equals(pe.unit()) ? (netPrice * pe.value() / 100.0) : pe.value();

				if (Boolean.TRUE.equals(row.getIsConsolidatedSum()) && Boolean.TRUE.equals(row.getHasFP())) {
					// Consolidated sum row → multiply by fpPalletCount (integer)
					palletExchange = perPallet * optInt(row.getFpPalletCount());
				} else if ("FP".equalsIgnoreCase(safeString(row.getPackagingType()))) {
					// Base rows → only FP packages get Palettentausch and use integer PalletCount
					palletExchange = perPallet * safeInt(row.getPalletCount());
				}
			}

			ExtraTermSpec gb = findTerm(mergedExtras, "Gitterboxtausch");
			if (gb != null && "GP".equalsIgnoreCase(safeString(row.getPackagingType()))) {
				// Only GP gets Gitterboxtausch and uses integer PalletCount
				double perBox = "%".equals(gb.unit()) ? (netPrice * gb.value() / 100.0) : gb.value();
				palletBoxExchange = perBox * safeInt(row.getPalletCount());
			}

			// Flag-driven extras (tiers/values aware)
			Map<String, Double> extrasMap = new LinkedHashMap<>();
			putIfPositive(extrasMap, "TailLiftSurcharge",
					computeExtraCost("Hebebühnenzuschlag", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getExpressNextDay()))
				putIfPositive(extrasMap, "ExpressNextDay",
						computeExtraCost("Express Next Day", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getExpress12()))
				putIfPositive(extrasMap, "Express12",
						computeExtraCost("Express 12:00 Uhr", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getExpress10()))
				putIfPositive(extrasMap, "Express10",
						computeExtraCost("Express 10:00 Uhr", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getExpress8()))
				putIfPositive(extrasMap, "Express8",
						computeExtraCost("Express 08:00 Uhr", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getFixtermin()))
				putIfPositive(extrasMap, "Fixtermin", computeExtraCost("Fixtermin", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getEmailAvis()))
				putIfPositive(extrasMap, "EmailAvis", computeExtraCost("E-Mail Avis", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getPhoneAvis()))
				putIfPositive(extrasMap, "PhoneAvis",
						computeExtraCost("Telefonisches Avis", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getBookingInAvis()))
				putIfPositive(extrasMap, "BookingInAvis",
						computeExtraCost("Booking in Avis", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getDangerousGoodsSurcharge()))
				putIfPositive(extrasMap, "DangerousGoodsSurcharge",
						computeExtraCost("Gefahrgutzuschlag", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getLongGoodsSurcharge()))
				putIfPositive(extrasMap, "LongGoodsSurcharge",
						computeExtraCost("Langgutzuschlag", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getShortWeekSurcharge()))
				putIfPositive(extrasMap, "ShortWeekSurcharge",
						computeExtraCost("Kurzwochenzuschlag", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getCarrierCertificate()))
				putIfPositive(extrasMap, "CarrierCertificate",
						computeExtraCost("Spediteurbescheinigung", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getB2cNationalSurcharge()))
				putIfPositive(extrasMap, "B2CNationalSurcharge",
						computeExtraCost("B2C Zuschlag (national)", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getB2cInternationalSurcharge()))
				putIfPositive(extrasMap, "B2CInternationalSurcharge",
						computeExtraCost("B2C Zuschlag (international)", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getSecurityFee()))
				putIfPositive(extrasMap, "SecurityFee", computeExtraCost("Security Fee", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getInsurance()))
				putIfPositive(extrasMap, "Insurance", computeExtraCost("Versicherung", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getPortiPapiere()))
				putIfPositive(extrasMap, "PortiPapiere",
						computeExtraCost("Porti/Papiere", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getCustom1()))
				putIfPositive(extrasMap, "Custom1", computeExtraCost("Eigene 1", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getCustom2()))
				putIfPositive(extrasMap, "Custom2", computeExtraCost("Eigene 2", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getCustom3()))
				putIfPositive(extrasMap, "Custom3", computeExtraCost("Eigene 3", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getCustom4()))
				putIfPositive(extrasMap, "Custom4", computeExtraCost("Eigene 4", mergedExtras, netPrice, row));
			if (Boolean.TRUE.equals(row.getCustom5()))
				putIfPositive(extrasMap, "Custom5", computeExtraCost("Eigene 5", mergedExtras, netPrice, row));
			// ✅ Add Palettentausch / Gitterboxtausch amounts into the per-row extra costs
			// map
			if (palletExchange > 0) {
				extrasMap.put("PalletExchange", round2(palletExchange));
			}
			if (palletBoxExchange > 0) {
				extrasMap.put("PalletBoxExchange", round2(palletBoxExchange));
			}

			// Totals
			row.setPrice(round2(netPrice));
			row.setToll(round2(totalToll));
			row.setTollPercent(round2(tollValue));
			row.setDiesel(round2(dieselEuro));
			row.setDieselPercent(round2(dieselPercent));

			// Persist per-row extras (TS parity)
			row.setExtraCosts(new LinkedHashMap<>(extrasMap));

			// Totals (these two are already included via extrasMap)
			double extrasSum = extrasMap.values().stream().mapToDouble(ShipmentSummaryService::round2).sum();

			double totalExtraCosts = round2(totalToll + dieselEuro + extrasSum);
			double totalRowPrice = round2(netPrice + totalExtraCosts);

			row.setExtraCostsTotalPrice(totalExtraCosts);
			row.setTotalPrice(totalRowPrice);

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
		} finally {
			unbindRawExtraCosts();
		}
	}

	// ---------------------------------------------------------------------
	// Extra costs (merge & lookup) + Diesel Floater config/matrix
	// ---------------------------------------------------------------------

	public record ExtraTermSpec(String term, String unit, double value) {
	}

	public record DieselFloaterConfig(String source, List<Map<String, Double>> brackets) {
	}

	private List<ExtraTermSpec> getMergedExtraCost(Map<String, Object> extraCostsCatalog, String country) {
		if (extraCostsCatalog == null || extraCostsCatalog.isEmpty())
			return List.of();

		Map<String, Object> intNode = getCountryNode(extraCostsCatalog, "INT");
		Map<String, Object> countryNode = getCountryNode(extraCostsCatalog, country);

		List<Map<String, Object>> intBase = normalizeBaseOrAdditional(intNode.get("Base"));
		List<Map<String, Object>> intAdd = normalizeBaseOrAdditional(intNode.get("Additional"));
		List<Map<String, Object>> cBase = normalizeBaseOrAdditional(countryNode.get("Base"));
		List<Map<String, Object>> cAdd = normalizeBaseOrAdditional(countryNode.get("Additional"));

		Map<String, ExtraTermSpec> byTerm = new LinkedHashMap<>();
		for (Map<String, Object> m : intBase)
			putTerm(byTerm, m);
		for (Map<String, Object> m : intAdd)
			putTerm(byTerm, m);
		for (Map<String, Object> m : cBase)
			putTerm(byTerm, m);
		for (Map<String, Object> m : cAdd)
			putTerm(byTerm, m);

		return new ArrayList<>(byTerm.values());
	}

	private ExtraTermSpec findExtraTerm(Map<String, Object> extraCosts, String country, String term) {
		if (extraCosts == null || term == null)
			return null;

		Map<String, Object> countryNode = getCountryNode(extraCosts, country);
		if (countryNode.isEmpty())
			return null;

		List<Map<String, Object>> base = normalizeBaseOrAdditional(countryNode.get("Base"));
		for (Map<String, Object> item : base) {
			String t = safeString(item.get("Term"));
			if (term.equals(t)) {
				double value = toDouble(item.get("Value"));
				String unit = safeString(item.getOrDefault("Unit", "€"));
				return new ExtraTermSpec(term, unit, value);
			}
		}

		List<Map<String, Object>> additional = normalizeBaseOrAdditional(countryNode.get("Additional"));
		for (Map<String, Object> item : additional) {
			String t = safeString(item.get("Term"));
			if (term.equals(t)) {
				double value = toDouble(item.get("Value"));
				String unit = safeString(item.getOrDefault("Unit", "€"));
				return new ExtraTermSpec(term, unit, value);
			}
		}

		Map<String, Object> intNode = getCountryNode(extraCosts, "INT");
		List<Map<String, Object>> intBase = normalizeBaseOrAdditional(intNode.get("Base"));
		for (Map<String, Object> item : intBase) {
			String t = safeString(item.get("Term"));
			if (term.equals(t)) {
				double value = toDouble(item.get("Value"));
				String unit = safeString(item.getOrDefault("Unit", "€"));
				return new ExtraTermSpec(term, unit, value);
			}
		}

		return null;
	}

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
		if (term.isBlank())
			return;
		String unit = safeString(m.getOrDefault("Unit", "€"));
		double value = toDouble(m.get("Value"));
		byTerm.put(term, new ExtraTermSpec(term, unit, value));
	}

	private ExtraTermSpec findTerm(List<ExtraTermSpec> list, String term) {
		for (ExtraTermSpec s : list)
			if (term.equals(s.term()))
				return s;
		return null;
	}

	private void addFlagExtra(Map<String, Double> extras, List<ExtraTermSpec> list, Boolean flag, String term,
			double netPrice) {
		if (Boolean.TRUE.equals(flag)) {
			ExtraTermSpec spec = findTerm(list, term);
			if (spec != null) {
				double v = "%".equals(spec.unit()) ? (netPrice * spec.value() / 100.0) : spec.value();
				if (v > 0)
					extras.put(term, round2(v));
			}
		}
	}

	/**
	 * Extract DieselFloater config directly from extraCosts raw map (country node).
	 */
	private DieselFloaterConfig extractDieselFloaterConfig(Map<String, Object> extraCosts, String country) {
		Map<String, Object> countryNode = getCountryNode(extraCosts, country);
		if (countryNode.isEmpty())
			return null;

		Object dfObj = countryNode.get("DieselFloater");
		if (dfObj instanceof Map<?, ?> df) {
			String source = safeString(df.get("DieselFloaterSource"));
			Object vals = df.get("DieselFloaterValues");
			List<Map<String, Double>> brackets = normalizeDieselBrackets(vals);
			if (!source.isBlank() && !brackets.isEmpty()) {
				return new DieselFloaterConfig(source, brackets);
			}
		}

		List<Map<String, Object>> base = normalizeBaseOrAdditional(countryNode.get("Base"));
		for (Map<String, Object> item : base) {
			if ("Dieselzuschlag".equals(safeString(item.get("Term")))) {
				Object innerDfObj = item.get("DieselFloater");
				if (innerDfObj instanceof Map<?, ?> innerDf) {
					String source = safeString(innerDf.get("DieselFloaterSource"));
					Object vals = innerDf.get("DieselFloaterValues");
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
		if (!(vals instanceof List<?> list))
			return List.of();
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

	private static void addNumeric(Map<String, Double> sums, String prop, double delta) {
		sums.put(prop, sums.getOrDefault(prop, 0.0) + delta);
	}

	@SuppressWarnings("unchecked")
	private double getDieselFloaterMatrixValue(Map<String, Object> dieselFloaterMatrix, String shipmentDate,
			String source) {
		if (dieselFloaterMatrix == null || dieselFloaterMatrix.isEmpty())
			return 0.0;
		if (isBlank(shipmentDate) || isBlank(source))
			return 0.0;

		LocalDate date = parseFlexibleLocalDate(shipmentDate);
		if (date == null) {
			log.warn("getDieselFloaterMatrixValue: invalid shipmentDate '{}'", shipmentDate);
			return 0.0;
		}

		LocalDate prev = date.minusMonths(1);
		String yearKey = String.valueOf(prev.getYear());
		int monthIndex = prev.getMonthValue() - 1; // 0..11

		Object yearDataObj = dieselFloaterMatrix.get(yearKey);
		if (!(yearDataObj instanceof List<?> yearData) || yearData.isEmpty())
			return 0.0;
		if (monthIndex < 0 || monthIndex >= yearData.size())
			return 0.0;

		Object monthEntryObj = yearData.get(monthIndex);
		if (!(monthEntryObj instanceof Map<?, ?> monthEntry))
			return 0.0;

		double value = toDouble(monthEntry.get(source));
		log.debug("DieselFloaterMatrix: year={} monthIdx={} source={} value={}", yearKey, monthIndex, source, value);
		return value;
	}

	/**
     * Parses shipmentDate in multiple common formats and returns a LocalDate.
     * Supported examples:
     *  - 2025-08-11, 2025/08/11, 2025.08.11
     *  - 11.08.2025, 11/08/2025, 11-08-2025
     *  - 08/11/2025 (US)
     *  - 2025-08, 2025/08, 2025.08  (defaults day=1)
     *  - 20250811 (compact yyyymmdd)
     *
     * Returns null if no format matches.
     */
    private LocalDate parseFlexibleLocalDate(String shipmentDate) {
        if (isBlank(shipmentDate)) return null;

        String s = shipmentDate.trim();

        // 1) ISO-like with '-' delimiter first (your original default)
        //    Try yyyy-MM-dd strictly.
        try {
            String[] partsDash = s.split("-");
            if (partsDash.length >= 2) {
                int y = Integer.parseInt(partsDash[0]);
                int m = Integer.parseInt(partsDash[1]);
                int d = Math.min(partsDash.length > 2 ? Integer.parseInt(partsDash[2]) : 1, 28);
                return LocalDate.of(y, m, d);
            }
        } catch (Exception ignore) { /* try other patterns */ }

        // 2) Dot-delimited (e.g., 11.08.2025 or 2025.08.11)
        try {
            String[] partsDot = s.split("\\.");
            if (partsDot.length == 3) {
                // Try DD.MM.YYYY
                int d1 = Integer.parseInt(partsDot[0]);
                int m1 = Integer.parseInt(partsDot[1]);
                int y1 = Integer.parseInt(partsDot[2]);
                if (y1 >= 1000 && m1 >= 1 && m1 <= 12) {
                    int d = Math.min(d1, 28);
                    return LocalDate.of(y1, m1, d);
                }
                // Try YYYY.MM.DD
                int y2 = Integer.parseInt(partsDot[0]);
                int m2 = Integer.parseInt(partsDot[1]);
                int d2 = Integer.parseInt(partsDot[2]);
                int d = Math.min(d2, 28);
                return LocalDate.of(y2, m2, d);
            } else if (partsDot.length == 2) {
                // YYYY.MM (day=1)
                int y = Integer.parseInt(partsDot[0]);
                int m = Integer.parseInt(partsDot[1]);
                return LocalDate.of(y, m, 1);
            }
        } catch (Exception ignore) { /* try other patterns */ }

        // 3) Slash-delimited (11/08/2025 or 08/11/2025 or 2025/08/11)
        try {
            String[] partsSlash = s.split("/");
            if (partsSlash.length == 3) {
                // Try DD/MM/YYYY
                int d1 = Integer.parseInt(partsSlash[0]);
                int m1 = Integer.parseInt(partsSlash[1]);
                int y1 = Integer.parseInt(partsSlash[2]);
                if (y1 >= 1000 && m1 >= 1 && m1 <= 12) {
                    int d = Math.min(d1, 28);
                    return LocalDate.of(y1, m1, d);
                }
                // Try MM/DD/YYYY (US)
                int m2 = Integer.parseInt(partsSlash[0]);
                int d2 = Integer.parseInt(partsSlash[1]);
                int y2 = Integer.parseInt(partsSlash[2]);
                if (y2 >= 1000 && m2 >= 1 && m2 <= 12) {
                    int d = Math.min(d2, 28);
                    return LocalDate.of(y2, m2, d);
                }
                // Try YYYY/MM/DD
                int y3 = Integer.parseInt(partsSlash[0]);
                int m3 = Integer.parseInt(partsSlash[1]);
                int d3 = Integer.parseInt(partsSlash[2]);
                int d = Math.min(d3, 28);
                return LocalDate.of(y3, m3, d);
            } else if (partsSlash.length == 2) {
                // YYYY/MM (day=1)
                int y = Integer.parseInt(partsSlash[0]);
                int m = Integer.parseInt(partsSlash[1]);
                return LocalDate.of(y, m, 1);
            }
        } catch (Exception ignore) { /* try other patterns */ }

        // 4) Hyphen-delimited non-ISO like DD-MM-YYYY
        try {
            String[] p = s.split("-");
            if (p.length == 3) {
                // Try DD-MM-YYYY
                int d1 = Integer.parseInt(p[0]);
                int m1 = Integer.parseInt(p[1]);
                int y1 = Integer.parseInt(p[2]);
                if (y1 >= 1000 && m1 >= 1 && m1 <= 12) {
                    int d = Math.min(d1, 28);
                    return LocalDate.of(y1, m1, d);
                }
                // Try YYYY-MM-DD (already tried earlier, but keep as fallback)
                int y2 = Integer.parseInt(p[0]);
                int m2 = Integer.parseInt(p[1]);
                int d2 = Integer.parseInt(p[2]);
                int d = Math.min(d2, 28);
                return LocalDate.of(y2, m2, d);
            }
        } catch (Exception ignore) { /* try other patterns */ }

        // 5) Compact yyyymmdd
        try {
            if (s.matches("\\d{8}")) {
                int y = Integer.parseInt(s.substring(0, 4));
                int m = Integer.parseInt(s.substring(4, 6));
                int d = Math.min(Integer.parseInt(s.substring(6, 8)), 28);
                return LocalDate.of(y, m, d);
            }
        } catch (Exception ignore) { /* try other patterns */ }

        // 6) Year-Month with spaces or other delimiters (e.g., "2025 08")
        try {
            String norm = s.replace('.', '-').replace('/', '-').replace(' ', '-');
            String[] parts = norm.split("-");
            if (parts.length == 2 && parts[0].length() == 4) {
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                return LocalDate.of(y, m, 1);
            }
        } catch (Exception ignore) { /* give up */ }

        return null;
    }

	private double getDieselFloaterPercent(List<Map<String, Double>> brackets, double matrixValue) {
		if (brackets == null || brackets.isEmpty())
			return 0.0;
		List<Map.Entry<Double, Double>> flat = new ArrayList<>();
		for (Map<String, Double> b : brackets) {
			for (Map.Entry<String, Double> e : b.entrySet()) {
				flat.add(Map.entry(toDouble(e.getKey()), e.getValue() == null ? 0.0 : e.getValue()));
			}
		}
		flat.sort(Comparator.comparingDouble(Map.Entry::getKey));
		for (Map.Entry<Double, Double> e : flat) {
			if (matrixValue <= e.getKey())
				return e.getValue();
		}
		return flat.isEmpty() ? 0.0 : flat.get(flat.size() - 1).getValue();
	}

	// ---------------------------------------------------------------------
	// Utility helpers
	// ---------------------------------------------------------------------

	private static double opt(Double d) {
		return d == null ? 0.0 : d;
	}

	private static int optInt(Integer i) {
		return i == null ? 0 : i;
	}

	private static double safeDouble(Double d) {
		return d == null ? 0.0 : d;
	}

	private void setError(ShipmentItemDocument row, String msg, int code) {
		row.setMessage(msg);
		row.setErrorType(code);
		row.setPrice(0.0);
		row.setToll(0.0);
		row.setDiesel(0.0);
		row.setExtraCosts(new LinkedHashMap<>());
		row.setExtraCostsTotalPrice(0.0);
		row.setTotalPrice(0.0);
	}

	private static int safeInt(Object o) {
		if (o == null)
			return 0;
		if (o instanceof Number n)
			return n.intValue();
		if (o instanceof String s) {
			try {
				return Integer.parseInt(s.trim());
			} catch (Exception ignore) {
				return 0;
			}
		}
		return 0;
	}

	private static double safeDouble(Object o) {
		if (o == null)
			return 0.0;
		if (o instanceof Number n)
			return n.doubleValue();
		if (o instanceof String s) {
			try {
				return Double.parseDouble(s.trim());
			} catch (Exception ignore) {
				return 0.0;
			}
		}
		return 0.0;
	}

	private static int safeInt(Integer i) {
		return i == null ? 0 : i;
	}

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
		if (dict == null || dict.isEmpty())
			return new HashMap<>();
		Object node = dict.get(country);
		if (node instanceof Map)
			return (Map<String, Object>) node;
		Object intl = dict.get("INT");
		return (intl instanceof Map) ? (Map<String, Object>) intl : new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getNode(Map<String, Object> parent, String key) {
		if (parent == null)
			return new HashMap<>();
		Object node = parent.get(key);
		return (node instanceof Map) ? (Map<String, Object>) node : new HashMap<>();
	}

	private Map<String, Object> firstNonNull(Map<String, Object> a, Map<String, Object> b) {
		return (a != null && !a.isEmpty()) ? a : (b != null ? b : new HashMap<>());
	}

	private static String safeString(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static boolean isZero(Double d) {
		return d == null || d.doubleValue() == 0.0;
	}

	private static double toDouble(Object v) {
		if (v == null)
			return 0.0;
		if (v instanceof Number n)
			return n.doubleValue();
		try {
			return Double.parseDouble(String.valueOf(v));
		} catch (Exception ignore) {
			return 0.0;
		}
	}

	private static double toFixed(double value, int digits) {
		double m = Math.pow(10.0, digits);
		return Math.round(value * m) / m;
	}

	private static double round2(double v) {
		return toFixed(v, 2);
	}

	private int resolveRowStackFactor(Integer stackFactor, boolean stackable) {
		if (!stackable)
			return 1;
		int sf = (stackFactor == null) ? 1 : stackFactor;
		if (sf < 1)
			return 1;
		return sf;
	}

	private double computeMinimumWeight(Map<String, Object> minWeight, String packagingType, int palletCount) {
		if (minWeight == null || packagingType == null)
			return 0.0;

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
		// Kilometer: we use unit price by zone, not multiplying chargeable
		default:
			return chargeable;
		}
	}

	private String findMatchedWeightKey(Set<String> keys, double normalizedChargeable) {
		List<Double> doubles = keys.stream().map(k -> {
			try {
				return Double.parseDouble(k);
			} catch (Exception e) {
				return Double.POSITIVE_INFINITY;
			}
		}).sorted().collect(Collectors.toList());
		for (Double d : doubles) {
			if (normalizedChargeable <= d)
				return d % 1 == 0 ? String.valueOf(d.intValue()) : String.valueOf(d);
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
		if (zipStr.isBlank())
			zipStr = "00000";
		try {
			return Integer.parseInt(zipStr.replaceAll("\\D", ""));
		} catch (Exception e) {
			return 0;
		}
	}

	private boolean zipInCodes(int zip, String codes) {
		String normalized = codes.replaceAll("[ /]", "");
		String[] parts = normalized.split(",");
		for (String part : parts) {
			if (part.isBlank())
				continue;
			if (part.contains("-")) {
				String[] range = part.split("-");
				int start = toZip(range[0], true);
				int end = toZip(range[1], false);
				if (zip == end || (zip >= start && zip <= end))
					return true;
			} else {
				int start = toZip(part, true);
				int end = toZip(part, false);
				if (zip == end || (zip >= start && zip <= end))
					return true;
			}
		}
		return false;
	}

	private int toZip(String s, boolean start) {
		String padded = s.trim();
		while (padded.length() < 5)
			padded += (start ? "0" : "9");
		try {
			return Integer.parseInt(padded);
		} catch (Exception e) {
			return 0;
		}
	}

	// ---------- NEW: zone & price helpers (TS parity) ----------

	private Map<String, Object> findBestZipZoneEntry(List<Map<String, Object>> zipCodesList, ShipmentItemDocument row) {
		String zipRaw = (row.getProjectType() != null && row.getProjectType() == 1)
				? safeString(row.getZipCodeShipper())
				: safeString(row.getZipCodeConsignee());
		String normalized = normalizeZipCodeInput(zipRaw);
		if (normalized.isEmpty())
			return null;

		boolean numeric = normalized.chars().allMatch(Character::isDigit);
		if (numeric) {
			// ✅ Use an effectively-final variable inside the lambda
			final int zipNum;
			try {
				zipNum = Integer.parseInt(normalized);
			} catch (Exception ignore) {
				// If parse fails, we still keep it final
				return null;
			}

			List<Map<String, Object>> matches = zipCodesList.stream()
					.filter(z -> zipInCodes(zipNum, safeString(z.get("Codes")))).collect(Collectors.toList());

			if (matches.isEmpty())
				return null;
			// mimic TS behavior: pick the "best" (we reversed earlier version, here take
			// the last)
			return matches.get(matches.size() - 1);
		} else {
			// alphanumeric: compute best score
			record Scored(Map<String, Object> entry, int score) {
			}
			List<Scored> scored = new ArrayList<>();
			for (Map<String, Object> z : zipCodesList) {
				String codes = safeString(z.get("Codes"));
				int best = bestAlphaScore(normalized, codes);
				if (best >= 0)
					scored.add(new Scored(z, best));
			}
			if (scored.isEmpty())
				return null;
			scored.sort((a, b) -> Integer.compare(b.score, a.score));
			return scored.get(0).entry;
		}
	}

	private int bestAlphaScore(String zip, String codes) {
		String[] tokens = codes.replace("/", ",").replace(";", ",").split(",");
		int best = -1;
		for (String t : tokens) {
			t = t.trim();
			if (t.isEmpty())
				continue;
			int score = alphaTokenScore(zip, t);
			best = Math.max(best, score);
		}
		return best;
	}

	private int alphaTokenScore(String zip, String tokenRaw) {
		String t = tokenRaw.trim().toUpperCase();
		if (t.isEmpty())
			return -1;
		if (t.contains("-")) {
			String[] parts = t.split("-");
			if (parts.length != 2)
				return -1;
			String a = parts[0], b = parts[1];
			if (a.chars().allMatch(Character::isDigit) && b.chars().allMatch(Character::isDigit))
				return -1;
			if (a.length() == b.length()) {
				String prefix = zip.length() >= a.length() ? zip.substring(0, a.length()) : "";
				if (prefix.isEmpty())
					return -1;
				if (prefix.compareTo(a) >= 0 && prefix.compareTo(b) <= 0)
					return a.length();
				return -1;
			} else {
				if (zip.startsWith(a))
					return a.length();
				if (zip.startsWith(b))
					return b.length();
				return -1;
			}
		}
		if (zip.equals(t))
			return 1000 + t.length();
		return zip.startsWith(t) ? t.length() : -1;
	}

	private String normalizeZipCodeInput(String v) {
		return safeString(v).toUpperCase().replaceAll("[\\s/-]+", "");
	}

	private Map<String, Object> findKilometerZone(List<Map<String, Object>> zipCodesList, double km) {
		List<Map<String, Object>> withMax = new ArrayList<>();
		for (Map<String, Object> z : zipCodesList) {
			String codes = safeString(z.get("Codes"));
			try {
				double maxKm = Double.parseDouble(codes.replace(",", "."));
				Map<String, Object> copy = new HashMap<>(z);
				copy.put("_MaxKm", maxKm);
				withMax.add(copy);
			} catch (Exception ignore) {
			}
		}
		withMax.sort(Comparator.comparingDouble(o -> toDouble(o.get("_MaxKm"))));
		for (Map<String, Object> e : withMax) {
			if (km <= toDouble(e.get("_MaxKm")))
				return e;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private double computeRowPrice(Map<String, Object> weightRow, String zoneId, String rateType,
			double normalizedChargeable) {
		Map<String, Object> prices = getNode(weightRow, "Prices");
		double unitPrice = toDouble(prices.get(zoneId));
		switch (safeString(rateType)) {
		case "Gewicht 100Kg":
		case "Gewicht 100Kg aufgerundet":
		case "Gewicht 10Kg aufgerundet":
		case "Kilogramm":
			return normalizedChargeable * unitPrice;
		case "Kilometer":
			return unitPrice;
		default:
			return unitPrice;
		}
	}

	private double applyMinMaxIfAny(double price, Map<String, Object> zipRange, String rateType) {
		if (!ALLOWED_MIN_MAX_TARIFFS.contains(safeString(rateType)))
			return price;
		double min = toDouble(zipRange.get("MinPrice"));
		double max = toDouble(zipRange.get("MaxPrice"));
		if (min > 0 && price < min)
			return min;
		if (max > 0 && price > max)
			return max;
		return price;
	}

	private boolean isTollIncluded(Map<String, Object> extraCosts, String country) {
		Map<String, Object> countryNode = getCountryNode(extraCosts, country);
		List<Map<String, Object>> base = normalizeBaseOrAdditional(countryNode.get("Base"));
		for (Map<String, Object> item : base) {
			if ("Maut".equals(safeString(item.get("Term")))) {
				Object inc = item.get("Included");
				return Boolean.TRUE.equals(inc);
			}
		}
		return false;
	}

	private static void putIfPositive(Map<String, Double> map, String key, double v) {
		if (v > 0)
			map.put(key, round2(v));
	}

	// ---------- Express/LongGoods "Values" bridging ----------

	private static final ThreadLocal<Map<String, Map<String, Object>>> currentRawExtraCostHolder = new ThreadLocal<>();

	@SuppressWarnings("unchecked")
	private void bindRawExtraCosts(Map<String, Object> extraCosts, String country) {
		Map<String, Object> cNode = getCountryNode(extraCosts, country);
		Map<String, Object> iNode = getCountryNode(extraCosts, "INT");
		Map<String, Map<String, Object>> index = new LinkedHashMap<>();
		for (Map<String, Object> item : normalizeBaseOrAdditional(cNode.get("Base"))) {
			index.put(safeString(item.get("Term")), item);
		}
		for (Map<String, Object> item : normalizeBaseOrAdditional(cNode.get("Additional"))) {
			index.put(safeString(item.get("Term")), item);
		}
		for (Map<String, Object> item : normalizeBaseOrAdditional(iNode.get("Base"))) {
			index.putIfAbsent(safeString(item.get("Term")), item);
		}
		currentRawExtraCostHolder.set(index);
	}

	private void unbindRawExtraCosts() {
		currentRawExtraCostHolder.remove();
	}

	private Map<String, Object> findRawExtraCost(String term) {
		return currentRawExtraCostHolder.get() != null ? currentRawExtraCostHolder.get().get(term) : null;
	}

	private double computeExtraCost(String term, List<ExtraTermSpec> mergedExtras, double rowNetPrice,
			ShipmentItemDocument row) {
		Map<String, Object> raw = findRawExtraCost(term);
		if (raw != null && raw.containsKey("Values")) {
			Object values = raw.get("Values");
			if (values instanceof Map) {
				if ("Langgutzuschlag".equals(term)) {
					Double matched = findLongGoodsPrice((Map<String, Object>) values, safeDouble(row.getLength()),
							safeDouble(row.getEffectiveWeight()));
					if (matched != null)
						return matched;
				} else {
					double weight = firstNonZero(safeDouble(row.getChargeableWeight()),
							safeDouble(row.getEffectiveWeight()), safeDouble(row.getWeightByCubicMeters()),
							safeDouble(row.getWeightByLoadingMeters()));
					Double matched = findExpressTierPriceFromValues((Map<String, Object>) values, weight);
					if (matched != null) {
						String unit = safeString(raw.getOrDefault("Unit", "€"));
						return "%".equals(unit) ? (rowNetPrice * matched / 100.0) : matched;
					}
				}
			}
		}
		ExtraTermSpec spec = findTerm(mergedExtras, term);
		if (spec == null)
			return 0.0;
		return "%".equals(spec.unit()) ? (rowNetPrice * spec.value() / 100.0) : spec.value();
	}

	private double firstNonZero(double... vals) {
		for (double v : vals)
			if (v > 0)
				return v;
		return 0.0;
	}

	@SuppressWarnings("unchecked")
	private Double findExpressTierPriceFromValues(Map<String, Object> values, double weight) {
		List<Map<String, Object>> tiers = new ArrayList<>();
		for (Object o : values.values()) {
			if (o instanceof List<?> list) {
				for (Object entry : list) {
					if (entry instanceof Map<?, ?> m) {
						double kg = toDouble(m.get("kg"));
						double price = toDouble(m.get("price"));
						if (kg > 0 && price > 0)
							tiers.add(Map.of("kg", kg, "price", price));
					}
				}
			}
		}
		tiers.sort(Comparator.comparingDouble(m -> toDouble(m.get("kg"))));
		Double matched = null;
		for (Map<String, Object> t : tiers) {
			double kg = toDouble(t.get("kg"));
			double p = toDouble(t.get("price"));
			if (weight >= kg)
				matched = p;
			else
				break;
		}
		return matched;
	}

	@SuppressWarnings("unchecked")
	private Double findLongGoodsPrice(Map<String, Object> matrix, double length, double weight) {
		List<Integer> lengths = matrix.keySet().stream().map(k -> {
			try {
				return Integer.parseInt(String.valueOf(k));
			} catch (Exception e) {
				return null;
			}
		}).filter(Objects::nonNull).sorted().collect(Collectors.toList());
		Integer bestL = null;
		for (Integer l : lengths)
			if (length >= l)
				bestL = l;
			else
				break;
		if (bestL == null)
			return null;
		Object arr = matrix.get(String.valueOf(bestL));
		if (!(arr instanceof List<?> list))
			return null;
		List<Map<String, Object>> kgList = new ArrayList<>();
		for (Object e : list) {
			if (e instanceof Map<?, ?> m) {
				kgList.add((Map<String, Object>) m);
			}
		}
		kgList.sort(Comparator.comparingDouble(m -> toDouble(m.get("kg"))));
		Double matched = null;
		for (Map<String, Object> m : kgList) {
			double kg = toDouble(m.get("kg"));
			double price = toDouble(m.get("price"));
			if (weight >= kg)
				matched = price;
			else
				break;
		}
		return matched;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> normalizeBaseOrAdditional(Object node) {
		if (node == null)
			return List.of();
		if (node instanceof List<?>)
			return (List<Map<String, Object>>) node;
		if (node instanceof Map<?, ?> m)
			return new ArrayList<>(((Map<String, Map<String, Object>>) m).values());
		return List.of();
	}

	// ---------------------------------------------------------------------
	// DTOs
	// ---------------------------------------------------------------------

	@Data
	public static class RowSummedTotal {
		private int shipmentDataLength;
		private double price; // net price
		private double tollPrice; // € or % of net
		private double dieselPrice; // sum per-row diesel
		private double totalExtraCostsPrice; // toll + diesel + consolidated extras
		private double totalPrice; // net + extras
		private Map<String, Double> allExtraCosts; // TS parity per-term breakdown
	}

	@Data
	@AllArgsConstructor
	public static class ShipmentTotalSummary {
		private Map<String, RowSummedTotal> countriesRowTotal;
		private double totalShipmentPrice;
	}

	@Data
	public static class SummaryInitResult {
		private ShipmentTotalSummary shipmentTotalSummary;
		private Map<String, List<ShipmentItemDocument>> consolidatedShipmentData;
		private Map<String, Object> fetchedShipperExtraCosts;
		private Map<String, Object> dieselFloaterMatrix;
	}
}