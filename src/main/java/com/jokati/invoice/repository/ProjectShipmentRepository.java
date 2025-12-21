
package com.jokati.invoice.repository;

import com.jokati.invoice.model.ProjectShipmentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProjectShipmentRepository extends MongoRepository<ProjectShipmentDocument, String> {
    Optional<ProjectShipmentDocument> findByProjectId(String projectId);
    void deleteByProjectId(String projectId);
}
