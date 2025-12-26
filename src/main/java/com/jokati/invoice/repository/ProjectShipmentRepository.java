
package com.jokati.invoice.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jokati.invoice.model.ProjectShipmentDocument;

public interface ProjectShipmentRepository extends MongoRepository<ProjectShipmentDocument, String> {
    Optional<ProjectShipmentDocument> findByProjectId(String projectId);
    void deleteByProjectId(String projectId);
}
