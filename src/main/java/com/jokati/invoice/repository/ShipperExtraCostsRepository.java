
package com.jokati.invoice.repository;

import com.jokati.invoice.model.ShipperExtraCosts;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShipperExtraCostsRepository extends MongoRepository<ShipperExtraCosts, String> {
}
