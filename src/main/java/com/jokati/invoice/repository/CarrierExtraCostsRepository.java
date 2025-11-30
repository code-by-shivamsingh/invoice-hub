
package com.jokati.invoice.repository;

import com.jokati.invoice.model.CarrierExtraCosts;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CarrierExtraCostsRepository extends MongoRepository<CarrierExtraCosts, String> {
}
