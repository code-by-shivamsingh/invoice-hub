package com.jokati.invoice.repository;

import java.util.Collection;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.jokati.invoice.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, ObjectId> {

    List<User> findByFirebaseId(String firebaseId);

    boolean existsByUserId(String userId);

	List<User> findByCompanyId(String companyId);


  
}

