
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
		return  repository.findByProjectId(projectId);
	}
	
	// Get country list by projectId
	public List<String> getCountriesByProjectId(String projectId) {

	    ShipperRates rates = repository.findByProjectId(projectId)
	            .orElseThrow(() -> new NoSuchElementException("Shipper rates not found for projectId: " + projectId));

	    return new ArrayList<>(rates.getRates().keySet());
	}

	// Get rate by country (default first country)
	public Object getRateByCountry(String projectId, String countryCode) {

	    ShipperRates rates = repository.findByProjectId(projectId)
	            .orElseThrow(() -> new NoSuchElementException("Shipper rates not found for projectId: " + projectId));

	    Map<String, Object> rateMap = rates.getRates();

	    // If countryCode not provided → take first country
	    if (countryCode == null || countryCode.isEmpty()) {
	        countryCode = rateMap.keySet().stream()
	                .findFirst()
	                .orElseThrow(() -> new NoSuchElementException("No countries configured"));
	    }

	    Object rate = rateMap.get(countryCode);

	    if (rate == null) {
	        throw new NoSuchElementException("Rate not configured for country: " + countryCode);
	    }

	    return rate;
	}
	
	
	/**
	 * Provides ZipCode-based paginated rates for a project and country,
	 * ensuring weight prices are filtered strictly by selected ZipCodes.
	 */

	public Object getRateByCountryPaginated(
	        String projectId,
	        String countryCode,
	        int page,
	        int size
	) {

	    ShipperRates rates = repository.findByProjectId(projectId)
	            .orElseThrow(() ->new NoSuchElementException(
                                      "Shipper rates not found for projectId: " + projectId));

	    Map<String, Object> rateMap = rates.getRates();

	    //  Default country if not provided
	    if (countryCode == null || countryCode.trim().isEmpty()) {
	        countryCode = rateMap.keySet().stream()
	                .findFirst()
	                .orElseThrow(() ->new NoSuchElementException("No countries configured"));
	    }

	    Map<String, Object> countryRate =
	            (Map<String, Object>) rateMap.get(countryCode);

	    if (countryRate == null) {
	        throw new NoSuchElementException( "Rate not configured for country: " + countryCode);
	    }

	   
	    List<Map<String, Object>> zipCodes = (List<Map<String, Object>>) countryRate.get("ZipCodes");

	    if (zipCodes == null) {
	        zipCodes = List.of();
	    }

	    //Pagination counts (ZipCodes based)
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

	    // Selected ZipCodes for this page
	    List<Map<String, Object>> selectedZipCodes = zipCodes.stream()
	            .skip((long) page * size)
	            .limit(size)
	            .toList();

	    //ZipCode Ids (source of truth)
	    Set<String> allowedZipIds = selectedZipCodes.stream()
	            .map(z -> (String) z.get("Id"))
	            .collect(java.util.stream.Collectors.toSet());


	    Map<String, Object> weights = (Map<String, Object>) countryRate.get("Weights");

	    Map<String, Object> filteredWeights = new LinkedHashMap<>();

	    if (weights != null) {
	        for (Map.Entry<String, Object> entry : weights.entrySet()) {

	            Map<String, Object> weightData = (Map<String, Object>) entry.getValue();

	            if (weightData == null) continue;

	            Map<String, Object> prices = (Map<String, Object>) weightData.get("Prices");

	            if (prices == null || prices.isEmpty()) continue;

	            // ONLY prices matching ZipCode Ids
	            Map<String, Double> filteredPrices = prices.entrySet()
	                    .stream()
	                    .filter(e -> allowedZipIds.contains(e.getKey()))
	                    .collect(java.util.stream.Collectors.toMap(
	                            Map.Entry::getKey,
	                            e -> ((Number) e.getValue()).doubleValue(),
	                            (a, b) -> a,
	                            LinkedHashMap::new
	                    ));

	            Map<String, Object> newWeightData = new LinkedHashMap<>();
	            newWeightData.put("Id", weightData.get("Id"));
	            newWeightData.put("Prices", filteredPrices);

	            filteredWeights.put(entry.getKey(), newWeightData);
	        }
	    }

	 
	    Map<String, Object> response = new LinkedHashMap<>();
	    response.put("CountryCode", countryCode);
	    response.put("TariffType", countryRate.get("TariffType"));
	    response.put("Weights", filteredWeights);
	    response.put("ZipCodes", selectedZipCodes);
	    response.put("Pagination", pagination);

	    return response;
	}

}