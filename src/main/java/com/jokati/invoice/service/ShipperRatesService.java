
package com.jokati.invoice.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.jokati.invoice.dto.ShipperRateUpdateItemDTO;
import com.jokati.invoice.dto.ShipperRateUpdateRequestDTO;
import com.jokati.invoice.dto.ShipperRateZonePriceUpdateItemDTO;
import com.jokati.invoice.dto.ShipperRateZonePriceUpdateRequestDTO;
import com.jokati.invoice.dto.ShipperRatesRequestDTO;
import com.jokati.invoice.model.ShipperRates;
import com.jokati.invoice.repository.ShipperRatesRepository;

@Service
public class ShipperRatesService {

	private final ShipperRatesRepository repository;

	public ShipperRatesService(ShipperRatesRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public ShipperRates save(ShipperRates rates) {
		return repository.save(rates);
	}

	public Optional<ShipperRates> findById(String id) {
		return repository.findById(id);
	}

	@Transactional
	public ShipperRates update(String id, ShipperRates rates) {
		// Ensure the record exists; throw 404-like domain exception
		var existing = repository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Shipper rates not found for id: " + id));

		// Overwrite fields as per your logic
		existing.setProjectId(rates.getProjectId());
		existing.setRates(rates.getRates());

		return repository.save(existing);
	}

	@Transactional
	public void delete(String id) {
		if (!repository.existsById(id)) {
			throw new NoSuchElementException("Shipper rates not found for id: " + id);
		}
		repository.deleteById(id);
	}

	public Optional<ShipperRates> findByProjectId(String projectId) {
		// TODO Auto-generated method stub
		return repository.findByProjectId(projectId);
	}

	// Get country list by projectId
	public List<String> getCountriesByProjectId(String projectId) {

		return repository.findByProjectId(projectId).map(rates -> {
			if (rates.getRates() == null) {
				return List.<String>of();
			}
			return new ArrayList<>(rates.getRates().keySet());
		}).orElse(List.of());
	}

	// Get rate by country (default first country)
	public Object getRateByCountry(String projectId, String countryCode) {

		ShipperRates rates = repository.findByProjectId(projectId)
				.orElseThrow(() -> new NoSuchElementException("Shipper rates not found for projectId: " + projectId));

		Map<String, Object> rateMap = rates.getRates();

		// If countryCode not provided → take first country
		if (countryCode == null || countryCode.isEmpty()) {
			countryCode = rateMap.keySet().stream().findFirst()
					.orElseThrow(() -> new NoSuchElementException("No countries configured"));
		}

		Object rate = rateMap.get(countryCode);

		if (rate == null) {
			throw new NoSuchElementException("Rate not configured for country: " + countryCode);
		}

		return rate;
	}

	/**
	 * Provides ZipCode-based paginated rates for a project and country, ensuring
	 * weight prices are filtered strictly by selected ZipCodes.
	 */

	public Object getRateByCountryPaginated(String projectId, String countryCode, int page, int size) {

		return repository.findByProjectId(projectId).map(rates -> {

			Map<String, Object> rateMap = rates.getRates();
			if (rateMap == null || rateMap.isEmpty()) {
				return Map.of();
			}

			String resolvedCountryCode = countryCode;
			if (resolvedCountryCode == null || resolvedCountryCode.trim().isEmpty()) {
				resolvedCountryCode = rateMap.keySet().iterator().next();
			}

			Map<String, Object> countryRate = (Map<String, Object>) rateMap.get(resolvedCountryCode);
			if (countryRate == null) {
				return Map.of();
			}

			List<Map<String, Object>> zipCodes = (List<Map<String, Object>>) countryRate.get("ZipCodes");
			if (zipCodes == null) {
				zipCodes = List.of();
			}

			int totalElements = zipCodes.size();
			int totalPages = (int) Math.ceil((double) totalElements / size);
			boolean hasPrevious = page > 0;
			boolean hasNext = page < totalPages - 1;

			Map<String, Object> pagination = new LinkedHashMap<>();
			pagination.put("page", page);
			pagination.put("size", size);
			pagination.put("totalElements", totalElements);
			pagination.put("totalPages", totalPages);
			pagination.put("hasPrevious", hasPrevious);
			pagination.put("hasNext", hasNext);

			List<Map<String, Object>> selectedZipCodes = zipCodes.stream().skip((long) page * size).limit(size)
					.toList();

			Set<String> allowedZipIds = selectedZipCodes.stream().map(z -> (String) z.get("Id"))
					.collect(java.util.stream.Collectors.toSet());

			Map<String, Object> weights = (Map<String, Object>) countryRate.get("Weights");

			Map<String, Object> filteredWeights = new LinkedHashMap<>();

			if (weights != null) {
				for (Map.Entry<String, Object> entry : weights.entrySet()) {

					Map<String, Object> weightData = (Map<String, Object>) entry.getValue();
					if (weightData == null)
						continue;

					Map<String, Object> prices = (Map<String, Object>) weightData.get("Prices");
					if (prices == null || prices.isEmpty())
						continue;

					Map<String, Double> filteredPrices = prices.entrySet().stream()
							.filter(e -> allowedZipIds.contains(e.getKey()))
							.collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> {
								Object val = e.getValue();
								if (val == null)
									return null;

								if (val instanceof Number) {
									return ((Number) val).doubleValue();
								}

								// String like "29,1375" -> "29.1375"
								String s = val.toString().trim().replace(",", ".");
								return Double.parseDouble(s);
							}, (a, b) -> a, LinkedHashMap::new));

					Map<String, Object> newWeightData = new LinkedHashMap<>();
					newWeightData.put("Id", weightData.get("Id"));
					newWeightData.put("Prices", filteredPrices);

					filteredWeights.put(entry.getKey(), newWeightData);
				}
			}

			Map<String, Object> response = new LinkedHashMap<>();
			response.put("CountryCode", resolvedCountryCode);
			response.put("TariffType", countryRate.get("TariffType"));
			response.put("Weights", filteredWeights);
			response.put("ZipCodes", selectedZipCodes);
			response.put("Pagination", pagination);

			return response;
		}).orElse(Map.of());
	}

	@Transactional
	public void updateVisibleRates(ShipperRateUpdateRequestDTO request) {

		ShipperRates rates = repository.findFirstByProjectIdOrderByIdDesc(request.getProjectId()).orElseThrow(
				() -> new NoSuchElementException("Shipper rates not found for projectId: " + request.getProjectId()));

		Map<String, Object> rateMap = rates.getRates();
		if (rateMap == null || rateMap.isEmpty()) {
			throw new NoSuchElementException("No rates configured for this project");
		}

		Map<String, Object> countryRate = (Map<String, Object>) rateMap.get(request.getCountryCode());

		if (countryRate == null) {
			throw new NoSuchElementException("Country not found: " + request.getCountryCode());
		}

		Map<String, Object> weights = (Map<String, Object>) countryRate.get("Weights");

		if (weights == null) {
			return; // nothing to update
		}

		// UPDATE ONLY WHAT FRONTEND SENT
		for (ShipperRateUpdateItemDTO item : request.getUpdates()) {

			Map<String, Object> weightData = (Map<String, Object>) weights.get(item.getWeight());

			if (weightData == null)
				continue;

			Map<String, Object> prices = (Map<String, Object>) weightData.get("Prices");

			if (prices == null)
				continue;

			prices.put(item.getZipCodeId(), item.getPrice());
		}

		repository.save(rates);
	}

	@Transactional
	public ShipperRates createOrUpdateCountryWise(ShipperRatesRequestDTO request) {

		if (request == null) {
			throw new IllegalArgumentException("Request must not be null");
		}
		if (!StringUtils.hasText(request.getProjectId())) {
			throw new IllegalArgumentException("projectId is required");
		}
		if (request.getRates() == null || request.getRates().isEmpty()) {
			throw new IllegalArgumentException("rates must not be empty");
		}

		// You mentioned: POST contains only one country at a time
		if (request.getRates().size() != 1) {
			throw new IllegalArgumentException("Only one country is allowed per request");
		}

		String projectId = request.getProjectId().trim();

		// Load existing document by projectId (if present)
		ShipperRates existing = repository.findByProjectId(projectId).orElse(null);

		// Get incoming single country entry
		Map.Entry<String, Object> incoming = request.getRates().entrySet().iterator().next();
		String incomingCountry = incoming.getKey();

		if (!StringUtils.hasText(incomingCountry)) {
			throw new IllegalArgumentException("Country code must not be blank");
		}

		String countryCodeTrimmed = incomingCountry.trim();

		// If no existing doc -> create new
		if (existing == null) {
			ShipperRates newDoc = ShipperRates.builder().projectId(projectId)
					.rates(new LinkedHashMap<>(request.getRates())).build();
			return repository.save(newDoc);
		}

		// Merge into existing rates map
		Map<String, Object> existingRates = existing.getRates();
		if (existingRates == null) {
			existingRates = new LinkedHashMap<>();
		}

		// Remove old key if same country exists in different case (DE vs de)
		String oldKey = existingRates.keySet().stream().filter(k -> k != null && k.equalsIgnoreCase(countryCodeTrimmed))
				.findFirst().orElse(null);

		if (oldKey != null) {
			existingRates.remove(oldKey);
		}

		// Put/overwrite the new rate payload for that country
		existingRates.put(countryCodeTrimmed, incoming.getValue());

		existing.setProjectId(projectId);
		existing.setRates(existingRates);

		return repository.save(existing);
	}

	@Transactional
	public void updateZonePricesBulk(ShipperRateZonePriceUpdateRequestDTO request) {

		if (request == null) {
			throw new IllegalArgumentException("Request must not be null");
		}
		if (!StringUtils.hasText(request.getProjectId())) {
			throw new IllegalArgumentException("projectId is required");
		}
		if (!StringUtils.hasText(request.getCountryCode())) {
			throw new IllegalArgumentException("countryCode is required");
		}
		if (request.getUpdates() == null || request.getUpdates().isEmpty()) {
			throw new IllegalArgumentException("updates must not be empty");
		}

		// ✅ Always update only ONE document per projectId (latest one)
		ShipperRates rates = repository.findFirstByProjectIdOrderByIdDesc(request.getProjectId().trim()).orElseThrow(
				() -> new NoSuchElementException("Shipper rates not found for projectId: " + request.getProjectId()));

		Map<String, Object> rateMap = rates.getRates();
		if (rateMap == null || rateMap.isEmpty()) {
			throw new NoSuchElementException("No rates configured for this project");
		}

		// ✅ Find actual country key ignoring case
		String actualCountryKey = rateMap.keySet().stream()
				.filter(k -> k != null && k.equalsIgnoreCase(request.getCountryCode().trim())).findFirst().orElse(null);

		if (actualCountryKey == null) {
			throw new NoSuchElementException("Country not found: " + request.getCountryCode());
		}

		Map<String, Object> countryRate = (Map<String, Object>) rateMap.get(actualCountryKey);
		if (countryRate == null) {
			throw new NoSuchElementException("Country rate data missing for: " + actualCountryKey);
		}

		Map<String, Object> weights = (Map<String, Object>) countryRate.get("Weights");
		if (weights == null) {
			throw new NoSuchElementException("Weights not configured for country: " + actualCountryKey);
		}

		// ✅ Bulk update: only touches incoming (weight, zipCodeId) prices
		for (ShipperRateZonePriceUpdateItemDTO item : request.getUpdates()) {
			if (item == null)
				continue;

			if (!StringUtils.hasText(item.getWeight())) {
				throw new IllegalArgumentException("weight must not be blank");
			}
			if (!StringUtils.hasText(item.getZipCodeId())) {
				throw new IllegalArgumentException("zipCodeId must not be blank");
			}

			String weightKey = item.getWeight().trim();
			String zipId = item.getZipCodeId().trim();

			Map<String, Object> weightData = (Map<String, Object>) weights.get(weightKey);
			if (weightData == null) {
				// Choose behavior:
				// A) throw (strict)
				// B) skip (lenient)
				// C) auto-create (advanced)
				//
				// ✅ As "expert safe default": throw so client knows it sent invalid weight.
				throw new NoSuchElementException("Weight not found: " + weightKey);
			}

			Map<String, Object> prices = (Map<String, Object>) weightData.get("Prices");
			if (prices == null) {
				prices = new LinkedHashMap<>();
				weightData.put("Prices", prices);
			}

			// Normalize numeric values (supports "29,1375" -> 29.1375)
			Object normalized = normalizePrice(item.getPrice());

			// ✅ Only update this one zone price; no other keys are modified
			prices.put(zipId, normalized);
		}

		repository.save(rates);
	}

	@Transactional
	public void syncCountryRateFromPaginatedPayload(String projectId, Map<String, Object> payload) {

		if (!StringUtils.hasText(projectId)) {
			throw new IllegalArgumentException("projectId is required");
		}
		if (payload == null || payload.isEmpty()) {
			throw new IllegalArgumentException("payload must not be empty");
		}

		// ✅ Always update the latest document for projectId
		ShipperRates ratesDoc = repository.findFirstByProjectIdOrderByIdDesc(projectId.trim())
				.orElseThrow(() -> new NoSuchElementException("Shipper rates not found for projectId: " + projectId));

		Map<String, Object> allCountries = ratesDoc.getRates();
		if (allCountries == null || allCountries.isEmpty()) {
			throw new NoSuchElementException("No rates configured for projectId: " + projectId);
		}

		// ✅ Pagination may or may not come -> IGNORE it (we never read it)
		// payload.get("Pagination") is intentionally not used

		String countryCode = asString(payload.get("CountryCode"));
		if (!StringUtils.hasText(countryCode)) {
			throw new IllegalArgumentException("CountryCode is required in payload");
		}
		countryCode = countryCode.trim();

		// Resolve stored country key ignoring case
		String actualCountryKey = resolveKeyIgnoreCase(allCountries, countryCode);
		if (actualCountryKey == null) {
			throw new NoSuchElementException("Country not found in stored rates: " + countryCode);
		}

		Map<String, Object> storedCountryRate = asMap(allCountries.get(actualCountryKey));
		if (storedCountryRate == null) {
			throw new NoSuchElementException("Invalid country structure for: " + actualCountryKey);
		}

		// ---- 1) TariffType can change ----
		if (payload.containsKey("TariffType")) {
			storedCountryRate.put("TariffType", payload.get("TariffType"));
		}

		// ---- 2) ZipCodes can change (Codes/Zone) ----
		// Update only incoming zip entries by Id; do not delete others.
		List<Map<String, Object>> incomingZipCodes = asListOfMaps(payload.get("ZipCodes"));
		if (incomingZipCodes != null) {
			List<Map<String, Object>> storedZipCodes = asListOfMaps(storedCountryRate.get("ZipCodes"));
			if (storedZipCodes == null) {
				storedZipCodes = new java.util.ArrayList<>();
			}

			Map<String, Map<String, Object>> storedZipIndex = new LinkedHashMap<>();
			for (Map<String, Object> z : storedZipCodes) {
				String id = asString(z.get("Id"));
				if (StringUtils.hasText(id))
					storedZipIndex.put(id.trim(), z);
			}

			for (Map<String, Object> incomingZip : incomingZipCodes) {
				if (incomingZip == null)
					continue;

				String zipId = asString(incomingZip.get("Id"));
				if (!StringUtils.hasText(zipId))
					continue;
				zipId = zipId.trim();

				Map<String, Object> existingZip = storedZipIndex.get(zipId);

				if (existingZip == null) {
					// Add new zip entry
					Map<String, Object> newZip = new LinkedHashMap<>();
					newZip.put("Id", zipId);
					if (incomingZip.containsKey("Codes"))
						newZip.put("Codes", incomingZip.get("Codes"));
					if (incomingZip.containsKey("Zone"))
						newZip.put("Zone", incomingZip.get("Zone"));
					storedZipCodes.add(newZip);
					storedZipIndex.put(zipId, newZip);
				} else {
					// Update only fields present
					if (incomingZip.containsKey("Codes"))
						existingZip.put("Codes", incomingZip.get("Codes"));
					if (incomingZip.containsKey("Zone"))
						existingZip.put("Zone", incomingZip.get("Zone"));
				}
			}

			storedCountryRate.put("ZipCodes", storedZipCodes);
		}

		// ---- 3) Weights can be added or updated ----
		// If new weight comes -> create it
		// For existing weight -> update only incoming Id / Prices keys
		Map<String, Object> incomingWeights = asMap(payload.get("Weights"));
		if (incomingWeights != null) {

			Map<String, Object> storedWeights = asMap(storedCountryRate.get("Weights"));
			if (storedWeights == null) {
				storedWeights = new LinkedHashMap<>();
				storedCountryRate.put("Weights", storedWeights);
			}

			for (Map.Entry<String, Object> incomingWeightEntry : incomingWeights.entrySet()) {
				String weightKey = incomingWeightEntry.getKey();
				if (!StringUtils.hasText(weightKey))
					continue;
				weightKey = weightKey.trim();

				Map<String, Object> incomingWeightData = asMap(incomingWeightEntry.getValue());
				if (incomingWeightData == null)
					continue;

				// ✅ If weight doesn't exist, create it
				Map<String, Object> storedWeightData = asMap(storedWeights.get(weightKey));
				if (storedWeightData == null) {
					storedWeightData = new LinkedHashMap<>();
					storedWeights.put(weightKey, storedWeightData);
				}

				// Update weight bucket Id if present
				if (incomingWeightData.containsKey("Id")) {
					storedWeightData.put("Id", incomingWeightData.get("Id"));
				}

				// Merge prices (update only incoming zipId keys)
				Map<String, Object> incomingPrices = asMap(incomingWeightData.get("Prices"));
				if (incomingPrices != null) {
					Map<String, Object> storedPrices = asMap(storedWeightData.get("Prices"));
					if (storedPrices == null) {
						storedPrices = new LinkedHashMap<>();
						storedWeightData.put("Prices", storedPrices);
					}

					for (Map.Entry<String, Object> priceEntry : incomingPrices.entrySet()) {
						String zipId = priceEntry.getKey();
						if (!StringUtils.hasText(zipId))
							continue;
						zipId = zipId.trim();

						Object normalized = normalizePrice(priceEntry.getValue());
						storedPrices.put(zipId, normalized);
					}
				}
			}

			storedCountryRate.put("Weights", storedWeights);
		}

		// Save back
		allCountries.put(actualCountryKey, storedCountryRate);
		ratesDoc.setRates(allCountries);

		repository.save(ratesDoc);
	}

	/* ----------------- helpers ----------------- */

	private String resolveKeyIgnoreCase(Map<String, Object> map, String key) {
		if (map == null || key == null)
			return null;
		return map.keySet().stream().filter(k -> k != null && k.equalsIgnoreCase(key)).findFirst().orElse(null);
	}

	private String asString(Object val) {
		return val == null ? null : val.toString();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(Object obj) {
		if (obj instanceof Map) {
			return (Map<String, Object>) obj;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> asListOfMaps(Object obj) {
		if (obj instanceof List) {
			List<?> list = (List<?>) obj;
			for (Object item : list) {
				if (item != null && !(item instanceof Map))
					return null;
			}
			return (List<Map<String, Object>>) obj;
		}
		return null;
	}

	private Object normalizePrice(Object price) {
		if (price == null)
			return null;

		if (price instanceof Number) {
			return ((Number) price).doubleValue();
		}

		String s = price.toString().trim();
		if (s.isEmpty())
			return null;

		s = s.replace(",", ".");
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return price;
		}
	}

}