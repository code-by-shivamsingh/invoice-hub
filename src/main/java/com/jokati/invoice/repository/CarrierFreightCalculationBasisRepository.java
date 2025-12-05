
package com.jokati.invoice.repository;

import com.jokati.invoice.model.CarrierFreightCalculationBasis;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CarrierFreightCalculationBasisRepository extends MongoRepository<CarrierFreightCalculationBasis, String> {
}