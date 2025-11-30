
package com.jokati.invoice.service;

import com.jokati.invoice.model.DieselFloater;
import com.jokati.invoice.repository.DieselFloaterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DieselFloaterService {

    private final DieselFloaterRepository repository;

    public DieselFloaterService(DieselFloaterRepository repository) {
        this.repository = repository;
    }

    public DieselFloater save(DieselFloater dieselFloater) {
        return repository.save(dieselFloater);
    }

    public List<DieselFloater> findAll() {
        return repository.findAll();
    }

    public DieselFloater update(String id, DieselFloater dieselFloater) {
        dieselFloater.setId(id);
        return repository.save(dieselFloater);
    }
}
