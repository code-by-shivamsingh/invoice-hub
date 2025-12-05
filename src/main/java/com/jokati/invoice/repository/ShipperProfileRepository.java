
package com.jokati.invoice.repository;

import com.jokati.invoice.model.ShipperProfile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for ShipperProfile entity.
 * Provides CRUD operations and can be extended with custom queries if needed.
 */
@Repository
public interface ShipperProfileRepository extends MongoRepository<ShipperProfile, ObjectId> {
    // You can add custom query methods here if needed, for example:
    // Optional<ShipperProfile> findByProjectName(String projectName);
}
