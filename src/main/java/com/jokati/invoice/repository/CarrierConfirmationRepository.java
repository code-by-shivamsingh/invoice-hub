
package com.jokati.invoice.repository;

import com.jokati.invoice.model.CarrierConfirmation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CarrierConfirmationRepository extends MongoRepository<CarrierConfirmation, String> {
}
