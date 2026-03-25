package com.jokati.invoice.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.ShipperExtraCostsRequestDTO;
import com.jokati.invoice.model.ShipperExtraCosts;
import com.jokati.invoice.repository.ShipperExtraCostsRepository;

/**
 * Service layer for creating/updating/retrieving Shipper extra-costs documents.
 *
 * Production hardening added: - Recursively sanitizes Map keys to ensure NO dot
 * ('.') reaches MongoDB field names. * Decimal-like keys (e.g., "147.05") are
 * written as "147,05" (comma) to remain human-friendly and locale-compatible
 * with your CSVs. * Non-decimal keys with dots fallback to replacing '.' ->
 * '_'. - Works with arbitrarily nested Map/List structures without altering
 * values.
 *
 * NOTE: Values are NOT modified here. Numeric parsing (comma/dot) should remain
 * in your calculation/CSV parsing layer (e.g., toDouble(...)).
 */
@Service
public class ShipperExtraCostsService {

	private final ShipperExtraCostsRepository repository;

	public ShipperExtraCostsService(ShipperExtraCostsRepository repository) {
		this.repository = repository;
	}

	// ---------------------------------------------------------------------
	// Mongo key-sanitization helpers (PRODUCTION SAFE)
	// ---------------------------------------------------------------------

	/**
	 * Recursively sanitize Maps/Lists so that Mongo NEVER sees a '.' in any Map
	 * key. - Decimal-like keys ("147.05") become "147,05" (comma) to match EU CSVs
	 * and avoid dots. - Other dotted keys use a conservative '.' -> '_'
	 * replacement. Values are passed through unchanged.
	 */
	@SuppressWarnings("unchecked")
	private Object sanitizeForMongo(Object node) {
		if (node == null)
			return null;

		// Map: sanitize keys & recurse on values
		if (node instanceof Map<?, ?> raw) {
			Map<String, Object> clean = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : raw.entrySet()) {
				String rawKey = (e.getKey() == null) ? null : String.valueOf(e.getKey());
				String safeKey = (rawKey == null) ? null : sanitizeKey(rawKey);
				clean.put(safeKey, sanitizeForMongo(e.getValue()));
			}
			return clean;
		}

		// List: recurse on elements
		if (node instanceof List<?> list) {
			List<Object> out = new ArrayList<>(list.size());
			for (Object item : list) {
				out.add(sanitizeForMongo(item));
			}
			return out;
		}

		// Everything else: leave as-is (String/Number/Boolean/POJO)
		return node;
	}

	/**
	 * Make a single Map key Mongo-safe. If the key looks like a number with
	 * optional decimal (either ',' or '.'), prefer comma for decimal separator
	 * (e.g., "147.05" -> "147,05"). Otherwise, replace dots with underscores as a
	 * conservative fallback.
	 */
	private String sanitizeKey(String key) {
		if (key == null || key.indexOf('.') == -1)
			return key;
		return looksLikeDecimalNumberKey(key) ? key.replace('.', ',') : key.replace('.', '_');
	}

	/**
	 * Returns true if the key is a decimal-like token: - optional minus sign,
	 * digits, optional single decimal separator (',' or '.') - e.g., "147",
	 * "147.05", "147,05", "-12,5"
	 */
	private boolean looksLikeDecimalNumberKey(String key) {
		String s = key.trim();
		return s.matches("^-?\\d+(?:[\\.,]\\d+)?$");
	}

	// ---------------------------------------------------------------------
	// CRUD operations (now sanitizing before persistence)
	// ---------------------------------------------------------------------

	@Transactional
	public ShipperExtraCosts save(ShipperExtraCosts costs) {
		Map<String, Object> sanitized = (Map<String, Object>) sanitizeForMongo(costs.getExtraCosts());
		costs.setExtraCosts(sanitized);
		return repository.save(costs);
	}

	public Optional<ShipperExtraCosts> findById(String id) {
		return repository.findById(id);
	}

	@Transactional
	public ShipperExtraCosts update(String id, ShipperExtraCosts costs) {
		var existing = repository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Extra costs not found for id: " + id));

		existing.setProjectId(costs.getProjectId());

		Map<String, Object> sanitized = (Map<String, Object>) sanitizeForMongo(costs.getExtraCosts());
		existing.setExtraCosts(sanitized);

		return repository.save(existing);
	}

	@Transactional
	public void delete(String id) {
		if (!repository.existsById(id)) {
			throw new NoSuchElementException("Extra costs not found for id: " + id);
		}
		repository.deleteById(id);
	}

	public Optional<ShipperExtraCosts> findByProjectId(String projectId) {
		return repository.findByProjectId(projectId);
	}

	public List<String> getCountriesByProjectId(String projectId) {
		return repository.findByProjectId(projectId).map(costs -> {
			if (costs.getExtraCosts() == null) {
				return List.<String>of();
			}
			return new ArrayList<>(costs.getExtraCosts().keySet());
		}).orElse(List.of());
	}

	public Object getExtraCostByCountry(String projectId, String countryCode) {
		Optional<ShipperExtraCosts> optionalCosts = repository.findByProjectId(projectId);
		if (optionalCosts.isEmpty()) {
			return null;
		}

		ShipperExtraCosts costs = optionalCosts.get();
		Map<String, Object> extraCosts = costs.getExtraCosts();

		// If countryCode not provided → take first country
		if (!StringUtils.hasText(countryCode)) {
			countryCode = extraCosts.keySet().stream().findFirst()
					.orElseThrow(() -> new NoSuchElementException("No countries configured"));
		}

		return extraCosts.get(countryCode);
	}

	@Transactional
	public ShipperExtraCosts createOrAppendCountryWise(ShipperExtraCostsRequestDTO request) {
		if (!StringUtils.hasText(request.getProjectId())) {
			throw new IllegalArgumentException("projectId is required");
		}
		if (request.getExtraCosts() == null || request.getExtraCosts().isEmpty()) {
			throw new IllegalArgumentException("extraCosts must not be empty");
		}

		// Find existing by projectId
		ShipperExtraCosts existing = repository.findByProjectId(request.getProjectId()).orElse(null);

		// Create new
		if (existing == null) {
			Map<String, Object> sanitizedAll = (Map<String, Object>) sanitizeForMongo(request.getExtraCosts());
			ShipperExtraCosts newDoc = ShipperExtraCosts.builder().projectId(request.getProjectId())
					.extraCosts(new LinkedHashMap<>(sanitizedAll)).build();
			return repository.save(newDoc);
		}

		// Merge into existing (case-insensitive country keys)
		Map<String, Object> existingMap = existing.getExtraCosts();
		if (existingMap == null) {
			existingMap = new LinkedHashMap<>();
		}

		for (Map.Entry<String, Object> entry : request.getExtraCosts().entrySet()) {
			String incomingCode = entry.getKey();
			if (!StringUtils.hasText(incomingCode))
				continue;
			String trimmed = incomingCode.trim();

			// remove old key if matches ignoring case (avoid DE + de duplicates)
			String oldKey = existingMap.keySet().stream().filter(k -> k != null && k.equalsIgnoreCase(trimmed))
					.findFirst().orElse(null);
			if (oldKey != null) {
				existingMap.remove(oldKey);
			}

			// sanitize the payload (deep) before storing
			Object sanitizedPayload = sanitizeForMongo(entry.getValue());
			existingMap.put(trimmed, sanitizedPayload);
		}

		existing.setExtraCosts(existingMap);
		existing.setProjectId(request.getProjectId());
		return repository.save(existing);
	}

	/**
	 * Filter extraCosts map for response so that only country codes passed in
	 * CURRENT request are returned.
	 */
	public Map<String, Object> filterExtraCostsForResponse(ShipperExtraCosts saved,
			Map<String, Object> requestExtraCosts) {
		Map<String, Object> savedMap = saved.getExtraCosts();
		if (savedMap == null || savedMap.isEmpty() || requestExtraCosts == null || requestExtraCosts.isEmpty()) {
			return new LinkedHashMap<>();
		}

		Map<String, Object> filtered = new LinkedHashMap<>();

		for (String reqKey : requestExtraCosts.keySet()) {
			if (!StringUtils.hasText(reqKey))
				continue;

			String actualKey = savedMap.keySet().stream().filter(k -> k != null && k.equalsIgnoreCase(reqKey.trim()))
					.findFirst().orElse(null);

			if (actualKey != null) {
				filtered.put(actualKey, savedMap.get(actualKey));
			}
		}

		return filtered;
	}

	/**
	 * Return the full document but extraCosts contains ONLY the requested country.
	 * If country does not exist, return full document with extraCosts = null. If
	 * projectId not found, return null.
	 */
	public ShipperExtraCosts getExtraCostsDocumentByCountry(String projectId, String countryCode) {
		Optional<ShipperExtraCosts> optional = repository.findByProjectId(projectId);
		if (optional.isEmpty()) {
			return null; // controller will return okEmpty
		}

		ShipperExtraCosts doc = optional.get();
		Map<String, Object> map = doc.getExtraCosts();

		if (map == null || map.isEmpty()) {
			return copyWithExtraCosts(doc, null);
		}

		if (!StringUtils.hasText(countryCode)) {
			String first = map.keySet().iterator().next();
			Map<String, Object> filtered = new LinkedHashMap<>();
			filtered.put(first, map.get(first));
			return copyWithExtraCosts(doc, filtered);
		}

		String actualKey = map.keySet().stream().filter(k -> k != null && k.equalsIgnoreCase(countryCode.trim()))
				.findFirst().orElse(null);

		if (actualKey == null) {
			return copyWithExtraCosts(doc, null);
		}

		Map<String, Object> filtered = new LinkedHashMap<>();
		filtered.put(actualKey, map.get(actualKey));
		return copyWithExtraCosts(doc, filtered);
	}

	@Transactional
	public ShipperExtraCosts createOrAppendSingleCountry(ShipperExtraCostsRequestDTO request) {
		if (!StringUtils.hasText(request.getProjectId())) {
			throw new IllegalArgumentException("projectId is required");
		}
		if (request.getExtraCosts() == null || request.getExtraCosts().isEmpty()) {
			throw new IllegalArgumentException("extraCosts must not be empty");
		}
		if (request.getExtraCosts().size() != 1) {
			throw new IllegalArgumentException("Only one country is allowed per request");
		}

		String projectId = request.getProjectId().trim();
		ShipperExtraCosts existing = repository.findByProjectId(projectId).orElse(null);

		if (existing == null) {
			Map<String, Object> sanitizedAll = (Map<String, Object>) sanitizeForMongo(request.getExtraCosts());
			ShipperExtraCosts newDoc = ShipperExtraCosts.builder().projectId(projectId)
					.extraCosts(new LinkedHashMap<>(sanitizedAll)).build();
			return repository.save(newDoc);
		}

		Map<String, Object> existingMap = existing.getExtraCosts();
		if (existingMap == null)
			existingMap = new LinkedHashMap<>();

		Map.Entry<String, Object> entry = request.getExtraCosts().entrySet().iterator().next();
		String incomingCode = entry.getKey();
		if (!StringUtils.hasText(incomingCode)) {
			throw new IllegalArgumentException("Country code must not be blank");
		}
		String trimmedCode = incomingCode.trim();

		String oldKey = existingMap.keySet().stream().filter(k -> k != null && k.equalsIgnoreCase(trimmedCode))
				.findFirst().orElse(null);
		if (oldKey != null) {
			existingMap.remove(oldKey);
		}

		Object sanitizedPayload = sanitizeForMongo(entry.getValue());
		existingMap.put(trimmedCode, sanitizedPayload);

		existing.setProjectId(projectId);
		existing.setExtraCosts(existingMap);
		return repository.save(existing);
	}

	public Map<String, Object> filterExtraCostsForPostResponse(ShipperExtraCosts saved,
			Map<String, Object> requestExtraCosts) {
		if (saved == null || saved.getExtraCosts() == null || saved.getExtraCosts().isEmpty()
				|| requestExtraCosts == null || requestExtraCosts.isEmpty()) {
			return new LinkedHashMap<>();
		}

		String reqKey = requestExtraCosts.keySet().iterator().next();
		if (!StringUtils.hasText(reqKey)) {
			return new LinkedHashMap<>();
		}

		String actualKey = saved.getExtraCosts().keySet().stream()
				.filter(k -> k != null && k.equalsIgnoreCase(reqKey.trim())).findFirst().orElse(null);

		if (actualKey == null) {
			return new LinkedHashMap<>();
		}

		Map<String, Object> filtered = new LinkedHashMap<>();
		filtered.put(actualKey, saved.getExtraCosts().get(actualKey));
		return filtered;
	}

	/**
	 * NEW GET behavior: - If projectId exists and country exists -> return doc with
	 * only that country - If country does not exist -> return doc with extraCosts =
	 * {} (empty) - If projectId not found -> return null (controller returns
	 * okEmpty)
	 */
	public ShipperExtraCosts getDocumentByProjectIdAndCountry(String projectId, String countryCode) {
		if (!StringUtils.hasText(projectId)) {
			throw new IllegalArgumentException("projectId is required");
		}

		Optional<ShipperExtraCosts> optional = repository.findByProjectId(projectId.trim());
		if (optional.isEmpty()) {
			return null;
		}

		ShipperExtraCosts doc = optional.get();
		Map<String, Object> map = doc.getExtraCosts();

		if (map == null || map.isEmpty()) {
			return copyWithExtraCosts(doc, new LinkedHashMap<>()); // empty {}
		}

		if (!StringUtils.hasText(countryCode)) {
			return copyWithExtraCosts(doc, new LinkedHashMap<>()); // as per your comment
		}

		String actualKey = map.keySet().stream().filter(k -> k != null && k.equalsIgnoreCase(countryCode.trim()))
				.findFirst().orElse(null);

		if (actualKey == null) {
			return copyWithExtraCosts(doc, new LinkedHashMap<>());
		}

		Map<String, Object> filtered = new LinkedHashMap<>();
		filtered.put(actualKey, map.get(actualKey));

		return copyWithExtraCosts(doc, filtered);
	}

	private ShipperExtraCosts copyWithExtraCosts(ShipperExtraCosts source, Map<String, Object> extraCosts) {
		return ShipperExtraCosts.builder().id(source.getId()).projectId(source.getProjectId()).extraCosts(extraCosts) // may
																														// be
																														// null
																														// or
																														// {}
				.build();
	}
}