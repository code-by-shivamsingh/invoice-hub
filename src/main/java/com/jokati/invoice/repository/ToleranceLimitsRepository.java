package com.jokati.invoice.repository;




import com.jokati.invoice.model.ToleranceLimits;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ToleranceLimitsRepository extends MongoRepository<ToleranceLimits, String> {
    Optional<ToleranceLimits> findByCompanyId(String companyId);
    boolean existsByCompanyId(String companyId);
    void deleteByCompanyId(String companyId);
}
