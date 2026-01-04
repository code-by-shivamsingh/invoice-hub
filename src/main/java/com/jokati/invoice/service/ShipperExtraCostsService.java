
package com.jokati.invoice.service;

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
}
