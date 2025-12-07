
package com.jokati.invoice.repository;

import com.jokati.invoice.model.CarrierFreightCalculationBasis;


import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CarrierFreightCalculationBasisRepository extends MongoRepository<CarrierFreightCalculationBasis, String> {
	 Optional<CarrierFreightCalculationBasis> findByProjectId(String projectId);
}