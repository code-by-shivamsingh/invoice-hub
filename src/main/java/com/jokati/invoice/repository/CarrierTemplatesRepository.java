
package com.jokati.invoice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.CarrierTemplates;

public interface CarrierTemplatesRepository extends MongoRepository<CarrierTemplates, String> {
}
