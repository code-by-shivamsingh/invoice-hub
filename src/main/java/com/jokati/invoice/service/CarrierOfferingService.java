
package com.jokati.invoice.service;

import com.jokati.invoice.model.CarrierOffering;
import com.jokati.invoice.repository.CarrierOfferingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarrierOfferingService {

    private final CarrierOfferingRepository repository;

    public CarrierOfferingService(CarrierOfferingRepository repository) {
        this.repository = repository;
    }

    public List<CarrierOffering> findByProjectId(String projectId) {
        return repository.findByProjectId(projectId);
    }

    /**
     * Upsert by carrierProjectId (id).
     */
    public CarrierOffering upsert(String id, CarrierOffering offering) {
        offering.setId(id);
        offering.setCarrierProjectId(id);
        return repository.save(offering);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
