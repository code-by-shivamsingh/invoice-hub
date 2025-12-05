package com.jokati.invoice.repository;



import com.jokati.invoice.model.ShipperCustomerData;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ShipperCustomerDataRepository extends MongoRepository<ShipperCustomerData, ObjectId> {
    Optional<ShipperCustomerData> findTopByOrderByUpdatedAtDesc();
}
