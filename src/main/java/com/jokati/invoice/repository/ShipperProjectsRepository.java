
package com.jokati.invoice.repository;

import com.jokati.invoice.model.ShipperProjects;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShipperProjectsRepository extends MongoRepository<ShipperProjects, String> {
}