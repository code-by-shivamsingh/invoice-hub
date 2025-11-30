
package com.jokati.invoice.repository;

import com.jokati.invoice.model.ShipperFreightCalculationBasis;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShipperFreightCalculationBasisRepository extends MongoRepository<ShipperFreightCalculationBasis, String> {
}
