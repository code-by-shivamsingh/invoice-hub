package com.jokati.invoice.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.jokati.invoice.model.AdminUser;

public interface AdminUserRepository extends MongoRepository<AdminUser, String> {
    Optional<AdminUser> findByEmail(String email); 
}