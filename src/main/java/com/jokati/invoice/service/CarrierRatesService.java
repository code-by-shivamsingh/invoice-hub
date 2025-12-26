
package com.jokati.invoice.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jokati.invoice.model.CarrierRates;
import com.jokati.invoice.repository.CarrierRatesRepository;

@Service
public class CarrierRatesService {

    private final CarrierRatesRepository repository;

    public CarrierRatesService(CarrierRatesRepository repository) {
        this.repository = repository;
    }

    public Optional<CarrierRates> findById(String id) {
        return repository.findById(id);
    }

    /**
     * Upsert by id (carrierProjectId). Equivalent to Mongoose findByIdAndUpdate with upsert:true.
     */
    public CarrierRates upsert(String id, CarrierRates doc) {
        doc.setId(id);
        return repository.save(doc);
    }

    public CarrierRates create(CarrierRates doc) {
        return repository.save(doc);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
