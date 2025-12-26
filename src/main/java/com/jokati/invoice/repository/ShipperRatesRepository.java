
package com.jokati.invoice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.ShipperRates;

public interface ShipperRatesRepository extends MongoRepository<ShipperRates, String> {
}
