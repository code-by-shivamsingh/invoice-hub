package com.jokati.invoice.repository;




import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.jokati.invoice.model.ToleranceLimits;

@Repository
public interface ToleranceLimitsRepository extends MongoRepository<ToleranceLimits, String> {
    Optional<ToleranceLimits> findByCompanyId(String companyId);
    boolean existsByCompanyId(String companyId);
    void deleteByCompanyId(String companyId);
}
