
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

	    return repository.findByProjectId(projectId)
	            .map(rates -> {
	                if (rates.getRates() == null) {
	                    return List.<String>of();
	                }
	                return new ArrayList<>(rates.getRates().keySet());
	            })
	            .orElse(List.of()); 
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

	 return repository.findByProjectId(projectId)
	      .map(rates -> {

	   Map<String, Object> rateMap = rates.getRates();
	   if (rateMap == null || rateMap.isEmpty()) {
	   return Map.of();
	       }

	       String resolvedCountryCode = countryCode;
	       if (resolvedCountryCode == null || resolvedCountryCode.trim().isEmpty()) {
	       resolvedCountryCode = rateMap.keySet().iterator().next();
	             }

	       Map<String, Object> countryRate =
          (Map<String, Object>) rateMap.get(resolvedCountryCode);
	        if (countryRate == null) {
	        return Map.of();
	                }

	      List<Map<String, Object>> zipCodes =
	     (List<Map<String, Object>>) countryRate.get("ZipCodes");
	        if (zipCodes == null) { zipCodes = List.of();
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

	    List<Map<String, Object>> selectedZipCodes = zipCodes.stream()
	            .skip((long) page * size)
	            .limit(size)
	            .toList();

	        Set<String> allowedZipIds = selectedZipCodes.stream()
	        .map(z -> (String) z.get("Id"))
	        .collect(java.util.stream.Collectors.toSet());

	     Map<String, Object> weights =  (Map<String, Object>) countryRate.get("Weights");

	        Map<String, Object> filteredWeights = new LinkedHashMap<>();

	            if (weights != null) {
	                    for (Map.Entry<String, Object> entry : weights.entrySet()) {

	        Map<String, Object> weightData = (Map<String, Object>) entry.getValue();
	                        if (weightData == null) continue;

	        Map<String, Object> prices = (Map<String, Object>) weightData.get("Prices");
	                        if (prices == null || prices.isEmpty()) continue;

	          Map<String, Double> filteredPrices = prices.entrySet()
	                       .stream()
	                       .filter(e -> allowedZipIds.contains(e.getKey()))
	                       .collect(java.util.stream.Collectors.toMap(
	                          Map.Entry::getKey, e -> {
	                      Object val = e.getValue();
	                      if (val == null) return null;

	                       if (val instanceof Number) {
	                              return ((Number) val).doubleValue();
	                   }

	                         // String like "29,1375" -> "29.1375"
	                          String s = val.toString()
	                         .trim()
	                         .replace(",", ".");
	                          return Double.parseDouble(s);
	                   },
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
	                response.put("CountryCode", resolvedCountryCode);
	                response.put("TariffType", countryRate.get("TariffType"));
	                response.put("Weights", filteredWeights);
	                response.put("ZipCodes", selectedZipCodes);
	                response.put("Pagination", pagination);

	                return response;
	            })
	            .orElse(Map.of());
	}




}