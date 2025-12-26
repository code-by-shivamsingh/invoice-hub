
package com.jokati.invoice.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jokati.invoice.model.CarrierConfirmation;
import com.jokati.invoice.repository.CarrierConfirmationRepository;

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

    // If you want 404: uncomment below and use in controller instead of the simple delete
    // public void deleteById(String id) {
    //     if (!repository.existsById(id)) {
    //         throw new java.util.NoSuchElementException("Carrier confirmation not found for id: " + id);
    //     }
    //     repository.deleteById(id);
    // }
}

