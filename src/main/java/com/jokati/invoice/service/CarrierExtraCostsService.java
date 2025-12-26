
package com.jokati.invoice.service;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.model.CarrierExtraCosts;
import com.jokati.invoice.repository.CarrierExtraCostsRepository;

@Service
public class CarrierExtraCostsService {

    private final CarrierExtraCostsRepository repository;

    public CarrierExtraCostsService(CarrierExtraCostsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CarrierExtraCosts save(CarrierExtraCosts costs) {
        return repository.save(costs);
    }

    public Optional<CarrierExtraCosts> findById(String id) {
        return repository.findById(id);
    }

    @Transactional
    public CarrierExtraCosts update(String id, CarrierExtraCosts costs) {
        // Ensure record exists; throw 404 via global handler if not
        CarrierExtraCosts existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Carrier extra costs not found for id: " + id));

        existing.setCarrierProjectId(costs.getCarrierProjectId());
        existing.setExtraCosts(costs.getExtraCosts());

        return repository.save(existing);
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Carrier extra costs not found for id: " + id);
        }
        repository.deleteById(id);
    }
}
