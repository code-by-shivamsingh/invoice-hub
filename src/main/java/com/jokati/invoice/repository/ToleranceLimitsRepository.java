package com.jokati.invoice.repository;




import com.jokati.invoice.model.ToleranceLimits;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ToleranceLimitsRepository extends MongoRepository<ToleranceLimits, String> {
    Optional<ToleranceLimits> findByUserId(String userId);
    boolean existsByUserId(String userId);
    void deleteByUserId(String userId);
}
