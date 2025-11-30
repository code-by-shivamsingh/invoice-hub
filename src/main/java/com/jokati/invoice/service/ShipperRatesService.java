
package com.jokati.invoice.service;

import com.jokati.invoice.model.ShipperRates;
import com.jokati.invoice.repository.ShipperRatesRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShipperRatesService {

    private final ShipperRatesRepository repository;

    public ShipperRatesService(ShipperRatesRepository repository) {
        this.repository = repository;
    }

    public ShipperRates save(ShipperRates rates) {
        return repository.save(rates);
    }

    public Optional<ShipperRates> findById(String id) {
        return repository.findById(id);
    }

    public ShipperRates update(String id, ShipperRates rates) {
        rates.setId(id);
        return repository.save(rates);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}
