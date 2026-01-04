
package com.jokati.invoice.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.jokati.invoice.model.ShipperFreightCalculationBasis;

@Repository
public interface ShipperFreightCalculationBasisRepository
        extends MongoRepository<ShipperFreightCalculationBasis, ObjectId> {

    // You can add derived queries if needed, e.g.:
     Optional<ShipperFreightCalculationBasis> findByProjectId(String projectId);
}
