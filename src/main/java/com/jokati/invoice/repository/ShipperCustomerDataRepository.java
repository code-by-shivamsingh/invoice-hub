package com.jokati.invoice.repository;



import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.ShipperCustomerData;

public interface ShipperCustomerDataRepository extends MongoRepository<ShipperCustomerData, ObjectId> {
    Optional<ShipperCustomerData> findTopByOrderByUpdatedAtDesc();
}
