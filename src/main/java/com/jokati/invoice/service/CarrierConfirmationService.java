
package com.jokati.invoice.service;

import com.jokati.invoice.model.CarrierConfirmation;
import com.jokati.invoice.repository.CarrierConfirmationRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CarrierConfirmationService {

    private final CarrierConfirmationRepository repository;

    public CarrierConfirmationService(CarrierConfirmationRepository repository) {
        this.repository = repository;
    }

    public Optional<CarrierConfirmation> findById(String id) {
        return repository.findById(id);
    }

    /**
     * Upsert by id (carrierProjectId). Equivalent to findByIdAndUpdate with upsert:true
     */
    public CarrierConfirmation upsert(String id, CarrierConfirmation doc) {
        doc.setId(id);
        return repository.save(doc);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
