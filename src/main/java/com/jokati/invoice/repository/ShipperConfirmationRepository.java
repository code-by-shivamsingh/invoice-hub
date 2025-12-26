
package com.jokati.invoice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.ShipperConfirmation;

public interface ShipperConfirmationRepository extends MongoRepository<ShipperConfirmation, String> {
}
