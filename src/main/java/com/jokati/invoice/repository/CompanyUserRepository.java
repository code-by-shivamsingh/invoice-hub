package com.jokati.invoice.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.jokati.invoice.model.CompanyUser;

@Repository
public interface CompanyUserRepository extends MongoRepository<CompanyUser, String> {

    boolean existsByEmailIgnoreCase(String email);

    List<CompanyUser> findAllByCompany(String company); 
}