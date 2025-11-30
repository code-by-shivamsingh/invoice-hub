package com.jokati.invoice.repository;


import com.jokati.invoice.model.ShipmentData;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ShipmentRepository extends MongoRepository<ShipmentData, String> {
    List<ShipmentData> findByProjectId(String projectId);
    void deleteByProjectId(String projectId);
}
