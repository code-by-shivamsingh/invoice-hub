
package com.jokati.invoice.service;

import com.jokati.invoice.model.CarrierExtraCosts;
import com.jokati.invoice.repository.CarrierExtraCostsRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CarrierExtraCostsService {

    private final CarrierExtraCostsRepository repository;

    public CarrierExtraCostsService(CarrierExtraCostsRepository repository) {
        this.repository = repository;
    }

    public CarrierExtraCosts save(CarrierExtraCosts costs) {
        return repository.save(costs);
    }

    public Optional<CarrierExtraCosts> findById(String id) {
        return repository.findById(id);
    }

    public CarrierExtraCosts update(String id, CarrierExtraCosts costs) {
        costs.setId(id);
        return repository.save(costs);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}
