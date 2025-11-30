package com.jokati.invoice.service;


import com.jokati.invoice.model.ShipmentData;
import com.jokati.invoice.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShipmentService {

    private final ShipmentRepository repository;

    public ShipmentService(ShipmentRepository repository) {
        this.repository = repository;
    }

    public List<ShipmentData> getShipmentData(String projectId) {
        return repository.findByProjectId(projectId);
    }

    public ShipmentData saveShipmentData(ShipmentData shipmentData) {
        return repository.save(shipmentData);
    }

    public void deleteByProjectId(String projectId) {
        repository.deleteByProjectId(projectId);
    }
}
