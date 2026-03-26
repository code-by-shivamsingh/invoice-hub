package com.jokati.invoice.repository;

import com.jokati.invoice.model.Company;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CompanyRepository extends MongoRepository<Company, ObjectId> {
    Optional<Company> findByCompanyId(String companyId);

}
