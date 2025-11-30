
package com.jokati.invoice.service;

import com.jokati.invoice.model.ShipperExtraCosts;
import com.jokati.invoice.repository.ShipperExtraCostsRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShipperExtraCostsService {

    private final ShipperExtraCostsRepository repository;

    public ShipperExtraCostsService(ShipperExtraCostsRepository repository) {
        this.repository = repository;
    }

    public ShipperExtraCosts save(ShipperExtraCosts costs) {
        return repository.save(costs);
    }

    public Optional<ShipperExtraCosts> findById(String id) {
        return repository.findById(id);
    }

    public ShipperExtraCosts update(String id, ShipperExtraCosts costs) {
        costs.setId(id);
        return repository.save(costs);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}
