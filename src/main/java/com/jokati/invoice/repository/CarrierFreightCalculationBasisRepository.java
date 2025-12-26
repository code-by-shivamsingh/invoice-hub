
package com.jokati.invoice.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.CarrierFreightCalculationBasis;

public interface CarrierFreightCalculationBasisRepository extends MongoRepository<CarrierFreightCalculationBasis, String> {
	 Optional<CarrierFreightCalculationBasis> findByProjectId(String projectId);
}