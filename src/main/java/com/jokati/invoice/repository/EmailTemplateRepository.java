package com.jokati.invoice.repository;


import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.EmailTemplate;

public interface EmailTemplateRepository extends MongoRepository<EmailTemplate, String> {
    Optional<EmailTemplate> findByName(String name);
    boolean existsByName(String name);
}

