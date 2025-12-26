
package com.jokati.invoice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.DieselFloater;

public interface DieselFloaterRepository extends MongoRepository<DieselFloater, String> {
}
