
package com.jokati.invoice.service;

import com.jokati.invoice.model.ShipperConfirmation;
import com.jokati.invoice.repository.ShipperConfirmationRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShipperConfirmationService {

    private final ShipperConfirmationRepository repository;

    public ShipperConfirmationService(ShipperConfirmationRepository repository) {
        this.repository = repository;
    }

    public Optional<ShipperConfirmation> findById(String id) {
        return repository.findById(id);
    }

    /**
     * Upsert by id (shipperProjectId). Equivalent to Mongoose findOneAndUpdate with upsert:true.
     */
    public ShipperConfirmation upsert(String id, ShipperConfirmation doc) {
        doc.setId(id);
        return repository.save(doc);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
