
package com.jokati.invoice.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.jokati.invoice.model.ShipperTemplate;

@Repository
public interface ShipperTemplateRepository extends MongoRepository<ShipperTemplate, ObjectId> {
    List<ShipperTemplate> findByUserId(String userId);
}
