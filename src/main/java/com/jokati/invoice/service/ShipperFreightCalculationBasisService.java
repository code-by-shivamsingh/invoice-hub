
package com.jokati.invoice.service;

import com.jokati.invoice.model.ShipperFreightCalculationBasis;
import com.jokati.invoice.repository.ShipperFreightCalculationBasisRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShipperFreightCalculationBasisService {

    private final ShipperFreightCalculationBasisRepository repository;

    public ShipperFreightCalculationBasisService(ShipperFreightCalculationBasisRepository repository) {
        this.repository = repository;
    }

    public ShipperFreightCalculationBasis save(ShipperFreightCalculationBasis basis) {
        return repository.save(basis);
    }

    public Optional<ShipperFreightCalculationBasis> findById(String id) {
        return repository.findById(id);
    }

    public ShipperFreightCalculationBasis update(String id, ShipperFreightCalculationBasis basis) {
        basis.setId(id);
        return repository.save(basis);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}
