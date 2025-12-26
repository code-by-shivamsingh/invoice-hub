
package com.jokati.invoice.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.model.CarrierFreightCalculationBasis;
import com.jokati.invoice.repository.CarrierFreightCalculationBasisRepository;

@Service
public class CarrierFreightCalculationBasisService {

    private final CarrierFreightCalculationBasisRepository repository;

    public CarrierFreightCalculationBasisService(CarrierFreightCalculationBasisRepository repository) {
        this.repository = repository;
    }

    public Optional<CarrierFreightCalculationBasis> findByCarrierProjectId(String id) {
        return repository.findById(id);
    }

    /** Upsert by id (carrierProjectId). Equivalent to Mongoose findByIdAndUpdate with upsert:true. */
    @Transactional
    public CarrierFreightCalculationBasis upsert(String id, CarrierFreightCalculationBasis doc) {
        doc.setId(id);
        return repository.save(doc);
    }

    @Transactional
    public CarrierFreightCalculationBasis create(CarrierFreightCalculationBasis doc) {
        return repository.save(doc);
    }

    @Transactional
    public void deleteById(String id) {
        // If you want 404 semantics, uncomment the guard below:
        // if (!repository.existsById(id)) {
        //     throw new NoSuchElementException("Carrier freight calculation basis not found for id: " + id);
        // }
        repository.deleteById(id);
    }

    public Optional<CarrierFreightCalculationBasis> findByProjectId(String id) {
        return repository.findByProjectId(id);
    }
}
