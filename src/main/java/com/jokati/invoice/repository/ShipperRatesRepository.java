
package com.jokati.invoice.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.ShipperRates;

public interface ShipperRatesRepository extends MongoRepository<ShipperRates, String> {

	Optional<ShipperRates> findByProjectId(String projectId);
}
