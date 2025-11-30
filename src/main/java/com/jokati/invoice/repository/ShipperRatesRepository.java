
package com.jokati.invoice.repository;

import com.jokati.invoice.model.ShipperRates;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShipperRatesRepository extends MongoRepository<ShipperRates, String> {
}
