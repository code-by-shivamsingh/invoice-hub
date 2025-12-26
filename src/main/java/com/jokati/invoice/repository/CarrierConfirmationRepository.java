
package com.jokati.invoice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.CarrierConfirmation;

public interface CarrierConfirmationRepository extends MongoRepository<CarrierConfirmation, String> {
}
