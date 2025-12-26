
package com.jokati.invoice.repository;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.jokati.invoice.model.ShipperProject;

@Repository
public interface ShipperProjectRepository extends MongoRepository<ShipperProject, ObjectId> {

    List<ShipperProject> findByUserId(String userId);

    /** Case-insensitive exact match on project name for a given userId */
    Optional<ShipperProject> findFirstByUserIdAndNameIgnoreCase(String userId, String name);

    /** All matches, case-insensitive exact, if you want to review multiples */
    List<ShipperProject> findByUserIdAndNameIgnoreCase(String userId, String name);
}

