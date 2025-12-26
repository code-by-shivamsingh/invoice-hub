
package com.jokati.invoice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.ShipperExtraCosts;

public interface ShipperExtraCostsRepository extends MongoRepository<ShipperExtraCosts, String> {
}
