
package com.jokati.invoice.repository;

import com.jokati.invoice.model.CarrierRates;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CarrierRatesRepository extends MongoRepository<CarrierRates, String> {
}

