
package com.jokati.invoice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.jokati.invoice.model.CarrierOffering;

@Repository
public interface CarrierOfferingRepository extends MongoRepository<CarrierOffering, String> {

    List<CarrierOffering> findByProjectId(String projectId);

    Optional<CarrierOffering> findOneByCarrierProjectId(String carrierProjectId);
}
