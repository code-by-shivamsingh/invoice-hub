
package com.jokati.invoice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.CarrierExtraCosts;

public interface CarrierExtraCostsRepository extends MongoRepository<CarrierExtraCosts, String> {
}
