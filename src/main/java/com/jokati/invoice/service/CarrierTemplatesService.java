
package com.jokati.invoice.service;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.jokati.invoice.model.CarrierTemplates;
import com.jokati.invoice.repository.CarrierTemplatesRepository;

@Service
public class CarrierTemplatesService {

    private final CarrierTemplatesRepository repository;
    private final MongoTemplate mongoTemplate;

    public CarrierTemplatesService(CarrierTemplatesRepository repository,
                                   MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Return first document (Node: result[0])
     */
    public CarrierTemplates findFirst() {
        List<CarrierTemplates> all = repository.findAll();
        return all.isEmpty() ? null : all.get(0);
    }

    /**
     * Drop collection then insert one new document.
     * Mirrors Node: CarrierTemplates.collection.drop() then save()
     */
    public CarrierTemplates replaceAll(CarrierTemplates doc) {
        mongoTemplate.dropCollection(CarrierTemplates.class);
        return repository.save(doc);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}
