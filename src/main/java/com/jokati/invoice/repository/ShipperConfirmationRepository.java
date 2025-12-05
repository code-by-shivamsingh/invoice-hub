
package com.jokati.invoice.repository;

import com.jokati.invoice.model.ShipperConfirmation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShipperConfirmationRepository extends MongoRepository<ShipperConfirmation, String> {
}
