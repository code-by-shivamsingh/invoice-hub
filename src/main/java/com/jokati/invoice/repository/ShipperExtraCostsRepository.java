
package com.jokati.invoice.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.ShipperExtraCosts;

public interface ShipperExtraCostsRepository extends MongoRepository<ShipperExtraCosts, String> {

	Optional<ShipperExtraCosts> findByProjectId(String projectId);
}
