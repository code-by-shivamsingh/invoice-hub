
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

@Service
public class ShipperExtraCostsService {

    private final ShipperExtraCostsRepository repository;

    public ShipperExtraCostsService(ShipperExtraCostsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ShipperExtraCosts save(ShipperExtraCosts costs) {
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
        existing.setExtraCosts(costs.getExtraCosts());

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
		// TODO Auto-generated method stub
		return repository.findByProjectId(projectId);
	}
	
	public List<String> getCountriesByProjectId(String projectId) {

	    return repository.findByProjectId(projectId)
	            .map(costs -> {
	                if (costs.getExtraCosts() == null) {return List.<String>of();
	                }
	                return new ArrayList<>(costs.getExtraCosts().keySet());
	            })
	            .orElse(List.of()); 
	}

	
	public Object getExtraCostByCountry(String projectId, String countryCode) {

	    Optional<ShipperExtraCosts> optionalCosts = repository.findByProjectId(projectId) ;
	    
	    if (optionalCosts.isEmpty()) {
            return null;
        }

	    ShipperExtraCosts costs = optionalCosts.get();
	    Map<String, Object> extraCosts = costs.getExtraCosts();

	    // If countryCode not provided → take first country
	    if (countryCode == null || countryCode.isEmpty()) {
	        countryCode = extraCosts.keySet().stream()
	                .findFirst()
	                .orElseThrow(() -> new NoSuchElementException("No countries configured"));
	    }

	    Object cost = extraCosts.get(countryCode);

	    return cost;
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

	    // If not exists -> create new
	    if (existing == null) {
	        ShipperExtraCosts newDoc = ShipperExtraCosts.builder()
	                .projectId(request.getProjectId())
	                .extraCosts(new LinkedHashMap<>(request.getExtraCosts()))
	                .build();

	        return repository.save(newDoc);
	    }

	    // Merge into existing map (case-insensitive key replacement)
	    Map<String, Object> existingMap = existing.getExtraCosts();
	    if (existingMap == null) {
	        existingMap = new LinkedHashMap<>();
	    }

	    for (Map.Entry<String, Object> entry : request.getExtraCosts().entrySet()) {
	        String incomingCode = entry.getKey();
	        if (!StringUtils.hasText(incomingCode)) {
	            continue;
	        }
	        String trimmed = incomingCode.trim();

	        // remove old key if matches ignoring case (avoid DE + de duplicates)
	        String oldKey = existingMap.keySet().stream()
	                .filter(k -> k != null && k.equalsIgnoreCase(trimmed))
	                .findFirst()
	                .orElse(null);

	        if (oldKey != null) {
	            existingMap.remove(oldKey);
	        }

	        // put/overwrite with the new value
	        existingMap.put(trimmed, entry.getValue());
	    }

	    existing.setExtraCosts(existingMap);
	    // projectId stays same but keep consistent
	    existing.setProjectId(request.getProjectId());

	    return repository.save(existing);
	}

	/**
	 * Filter extraCosts map for response so that only country codes passed in CURRENT request are returned.
	 */
	public Map<String, Object> filterExtraCostsForResponse(ShipperExtraCosts saved, Map<String, Object> requestExtraCosts) {

	    Map<String, Object> savedMap = saved.getExtraCosts();
	    if (savedMap == null || savedMap.isEmpty() || requestExtraCosts == null || requestExtraCosts.isEmpty()) {
	        return new LinkedHashMap<>();
	    }

	    Map<String, Object> filtered = new LinkedHashMap<>();

	    for (String reqKey : requestExtraCosts.keySet()) {
	        if (!StringUtils.hasText(reqKey)) continue;

	        String actualKey = savedMap.keySet().stream()
	                .filter(k -> k != null && k.equalsIgnoreCase(reqKey.trim()))
	                .findFirst()
	                .orElse(null);

	        if (actualKey != null) {
	            filtered.put(actualKey, savedMap.get(actualKey));
	        }
	    }

	    return filtered;
	}

	/**
	 * Return the full document but extraCosts contains ONLY the requested country.
	 * If country does not exist, return full document with extraCosts = null.
	 * If projectId not found, return null.
	 */
	public ShipperExtraCosts getExtraCostsDocumentByCountry(String projectId, String countryCode) {

	    Optional<ShipperExtraCosts> optional = repository.findByProjectId(projectId);
	    if (optional.isEmpty()) {
	        return null; // controller will return okEmpty
	    }

	    ShipperExtraCosts doc = optional.get();
	    Map<String, Object> map = doc.getExtraCosts();

	    // If no extraCosts at all -> return parent with extraCosts null
	    if (map == null || map.isEmpty()) {
	        return copyWithExtraCosts(doc, null);
	    }

	    if (!StringUtils.hasText(countryCode)) {
	        // If caller didn't send countryCode, pick first (optional behavior)
	        String first = map.keySet().iterator().next();
	        Map<String, Object> filtered = new LinkedHashMap<>();
	        filtered.put(first, map.get(first));
	        return copyWithExtraCosts(doc, filtered);
	    }

	    String actualKey = map.keySet().stream()
	            .filter(k -> k != null && k.equalsIgnoreCase(countryCode.trim()))
	            .findFirst()
	            .orElse(null);

	    if (actualKey == null) {
	        // ✅ Country not found -> return full doc but extraCosts null
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

	    // You said POST will have only one country at a time
	    if (request.getExtraCosts().size() != 1) {
	        throw new IllegalArgumentException("Only one country is allowed per request");
	    }

	    String projectId = request.getProjectId().trim();

	    ShipperExtraCosts existing = repository.findByProjectId(projectId).orElse(null);

	    // Create new if not exists
	    if (existing == null) {
	        ShipperExtraCosts newDoc = ShipperExtraCosts.builder()
	                .projectId(projectId)
	                .extraCosts(new LinkedHashMap<>(request.getExtraCosts()))
	                .build();
	        return repository.save(newDoc);
	    }

	    // Merge into existing extraCosts map
	    Map<String, Object> existingMap = existing.getExtraCosts();
	    if (existingMap == null) {
	        existingMap = new LinkedHashMap<>();
	    }

	    // single entry
	    Map.Entry<String, Object> entry = request.getExtraCosts().entrySet().iterator().next();
	    String incomingCode = entry.getKey();

	    if (!StringUtils.hasText(incomingCode)) {
	        throw new IllegalArgumentException("Country code must not be blank");
	    }

	    String trimmedCode = incomingCode.trim();

	    // Remove existing key if same ignoring case (avoid DE + de duplicates)
	    String oldKey = existingMap.keySet().stream()
	            .filter(k -> k != null && k.equalsIgnoreCase(trimmedCode))
	            .findFirst()
	            .orElse(null);

	    if (oldKey != null) {
	        existingMap.remove(oldKey);
	    }

	    // Put/overwrite the country payload
	    existingMap.put(trimmedCode, entry.getValue());

	    existing.setProjectId(projectId);
	    existing.setExtraCosts(existingMap);

	    return repository.save(existing);
	}

	/**
	 * For POST response: return only the country that came in current request.
	 * If somehow country not found -> return empty map {} (not null).
	 */
	public Map<String, Object> filterExtraCostsForPostResponse(ShipperExtraCosts saved,
	                                                           Map<String, Object> requestExtraCosts) {

	    if (saved == null || saved.getExtraCosts() == null || saved.getExtraCosts().isEmpty()
	            || requestExtraCosts == null || requestExtraCosts.isEmpty()) {
	        return new LinkedHashMap<>();
	    }

	    // request contains one country
	    String reqKey = requestExtraCosts.keySet().iterator().next();
	    if (!StringUtils.hasText(reqKey)) {
	        return new LinkedHashMap<>();
	    }

	    String actualKey = saved.getExtraCosts().keySet().stream()
	            .filter(k -> k != null && k.equalsIgnoreCase(reqKey.trim()))
	            .findFirst()
	            .orElse(null);

	    if (actualKey == null) {
	        return new LinkedHashMap<>();
	    }

	    Map<String, Object> filtered = new LinkedHashMap<>();
	    filtered.put(actualKey, saved.getExtraCosts().get(actualKey));
	    return filtered;
	}

	/**
	 * NEW GET behavior:
	 * - If projectId exists and country exists -> return doc with only that country
	 * - If country does not exist -> return doc with extraCosts = {} (empty)
	 * - If projectId not found -> return null (controller returns okEmpty)
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
	        // If countryCode missing, keep existing behavior? You asked countryCode is passed.
	        // We can return empty {} or first. Here returning empty {} to be safe.
	        return copyWithExtraCosts(doc, new LinkedHashMap<>());
	    }

	    String actualKey = map.keySet().stream()
	            .filter(k -> k != null && k.equalsIgnoreCase(countryCode.trim()))
	            .findFirst()
	            .orElse(null);

	    if (actualKey == null) {
	        // country not found -> empty map
	        return copyWithExtraCosts(doc, new LinkedHashMap<>());
	    }

	    Map<String, Object> filtered = new LinkedHashMap<>();
	    filtered.put(actualKey, map.get(actualKey));

	    return copyWithExtraCosts(doc, filtered);
	}

	private ShipperExtraCosts copyWithExtraCosts(ShipperExtraCosts source, Map<String, Object> extraCosts) {
	    return ShipperExtraCosts.builder()
	            .id(source.getId())
	            .projectId(source.getProjectId())
	            .extraCosts(extraCosts) // empty {} when not found
	            .build();
	}

}
