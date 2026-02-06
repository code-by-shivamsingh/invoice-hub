
package com.jokati.invoice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


}
