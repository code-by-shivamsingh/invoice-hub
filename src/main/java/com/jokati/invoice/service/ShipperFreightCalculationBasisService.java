
package com.jokati.invoice.service;

import com.jokati.invoice.dto.ShipperFreightCalculationBasisRequestDTO;
import com.jokati.invoice.model.ShipperFreightCalculationBasis;
import com.jokati.invoice.repository.ShipperFreightCalculationBasisRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShipperFreightCalculationBasisService {

    private final ShipperFreightCalculationBasisRepository repository;

    /**
     * Save a new entity (generates new ObjectId).
     */
    @Transactional
    public ShipperFreightCalculationBasis save(ShipperFreightCalculationBasis entity) {
        // Ensure id is generated when saving a new record
        if (entity.getId() == null) {
            entity.setId(new ObjectId());
        }
        return repository.save(entity);
    }

    /**
     * Update by document id (hex string) — safely converts to ObjectId.
     */
    @Transactional
    public ShipperFreightCalculationBasis update(String idHex, ShipperFreightCalculationBasis updatedFields) {
        ObjectId id = toObjectId(idHex);

        var existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Freight calculation basis not found for id: " + idHex));

        // Apply incoming fields (replace/overwrite semantics)
        existing.setProjectId(updatedFields.getProjectId());
        existing.setCarrierProjectId(updatedFields.getCarrierProjectId());
        existing.setCountries(updatedFields.getCountries());
        existing.setFirebaseId(updatedFields.getFirebaseId());
        existing.setExtra(updatedFields.getExtra());

        return repository.save(existing);
    }

    /**
     * Find by id (hex string) — returns Optional.
     */
    public Optional<ShipperFreightCalculationBasis> findByProjectId(String projectId) {
        try {
            ObjectId id = new ObjectId(projectId);
            return repository.findById(id);
        } catch (IllegalArgumentException e) {
            // invalid ObjectId format; mirror Node-style behavior by returning empty
            return Optional.empty();
        }
    }

    /**
     * Delete by id (hex string).
     */
    @Transactional
    public void delete(String idHex) {
        ObjectId id = toObjectId(idHex);
        repository.deleteById(id);
    }

    /* ---------- helpers ---------- */

    private ObjectId toObjectId(String idHex) {
        try {
            return new ObjectId(idHex);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ObjectId: " + idHex, e);
        }
    }

    /**
     * Utility to build entity from request DTO.
     */
    public ShipperFreightCalculationBasis fromRequestDTO(ShipperFreightCalculationBasisRequestDTO req) {
        return ShipperFreightCalculationBasis.builder()
                .projectId(req.getProjectId())
                .carrierProjectId(req.getCarrierProjectId())
                .countries(req.getCountries())
                .firebaseId(req.getFirebaseId())
                .extra(req.getExtra())
                .build();
    }
}
