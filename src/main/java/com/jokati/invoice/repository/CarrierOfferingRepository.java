
package com.jokati.invoice.repository;

import com.jokati.invoice.model.CarrierOffering;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CarrierOfferingRepository extends MongoRepository<CarrierOffering, String> {
    List<CarrierOffering> findByProjectId(String projectId);
    CarrierOffering findByCarrierProjectId(String carrierProjectId);
}
