
package com.jokati.invoice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.CarrierRates;

public interface CarrierRatesRepository extends MongoRepository<CarrierRates, String> {
}

