
package com.jokati.invoice.repository;

import com.jokati.invoice.model.ShipperTemplate;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipperTemplateRepository extends MongoRepository<ShipperTemplate, ObjectId> {
    List<ShipperTemplate> findByUserId(String userId);
}
