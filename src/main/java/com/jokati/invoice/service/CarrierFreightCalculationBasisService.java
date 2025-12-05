
package com.jokati.invoice.service;

import com.jokati.invoice.model.CarrierFreightCalculationBasis;
import com.jokati.invoice.repository.CarrierFreightCalculationBasisRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CarrierFreightCalculationBasisService {

    private final CarrierFreightCalculationBasisRepository repository;

    public CarrierFreightCalculationBasisService(CarrierFreightCalculationBasisRepository repository) {
        this.repository = repository;
    }

    public Optional<CarrierFreightCalculationBasis> findById(String id) {
        return repository.findById(id);
    }

    /**
     * Upsert by id (carrierProjectId). Equivalent to Mongoose findByIdAndUpdate with upsert:true.
     */
    public CarrierFreightCalculationBasis upsert(String id, CarrierFreightCalculationBasis doc) {
        doc.setId(id);
        return repository.save(doc);
    }

    public CarrierFreightCalculationBasis create(CarrierFreightCalculationBasis doc) {
        return repository.save(doc);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}