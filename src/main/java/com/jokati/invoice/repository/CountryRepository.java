package com.jokati.invoice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.Country;

public interface CountryRepository extends MongoRepository<Country, String> {
}
