
package com.jokati.invoice.repository;

import com.jokati.invoice.model.CarrierTemplates;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CarrierTemplatesRepository extends MongoRepository<CarrierTemplates, String> {
}
