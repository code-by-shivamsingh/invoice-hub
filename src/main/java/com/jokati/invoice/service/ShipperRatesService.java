
package com.jokati.invoice.service;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.model.ShipperRates;
import com.jokati.invoice.repository.ShipperRatesRepository;

@Service
public class ShipperRatesService {

    private final ShipperRatesRepository repository;

    public ShipperRatesService(ShipperRatesRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ShipperRates save(ShipperRates rates) {
        return repository.save(rates);
    }

    public Optional<ShipperRates> findById(String id) {
        return repository.findById(id);
    }

    @Transactional
    public ShipperRates update(String id, ShipperRates rates) {
        // Ensure the record exists; throw 404-like domain exception
        var existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Shipper rates not found for id: " + id));

        // Overwrite fields as per your logic
        existing.setProjectId(rates.getProjectId());
        existing.setRates(rates.getRates());

        return repository.save(existing);
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Shipper rates not found for id: " + id);
        }
        repository.deleteById(id);
    }
}