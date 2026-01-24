
package com.jokati.invoice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.ShipperFreightCalculationBasisRequestDTO;
import com.jokati.invoice.model.ShipperFreightCalculationBasis;
import com.jokati.invoice.repository.ShipperFreightCalculationBasisRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShipperFreightCalculationBasisService {

    private final ShipperFreightCalculationBasisRepository repository;

    /** Save a new entity (generates new ObjectId). */
    @Transactional
    public ShipperFreightCalculationBasis save(ShipperFreightCalculationBasis entity) {
        if (entity.getId() == null) {
            entity.setId(new ObjectId());
        }
        return repository.save(entity);
    }

    /** Update by document id (hex string). */
    @Transactional
    public ShipperFreightCalculationBasis update(String idHex, ShipperFreightCalculationBasis updatedFields) {
        ObjectId id = toObjectId(idHex);

        var existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Freight calculation basis not found for id: " + idHex));

        // Replace/overwrite semantics as per your logic
        existing.setProjectId(updatedFields.getProjectId());
        existing.setCarrierProjectId(updatedFields.getCarrierProjectId());
        existing.setCountries(updatedFields.getCountries());
        existing.setFirebaseId(updatedFields.getFirebaseId());
        existing.setExtra(updatedFields.getExtra());

        return repository.save(existing);
    }

    /** Find by id (hex string) — returns Optional; Node-style handled in controller. */
    public Optional<ShipperFreightCalculationBasis> findByProjectId(String projectId) {
        try {
            
            return repository.findByProjectId(projectId);
        } catch (IllegalArgumentException e) {
            // Invalid ObjectId format; mirror Node-style behavior by returning empty
            return Optional.empty();
        }
    }

    /** Delete by id (hex string). */
    @Transactional
    public void delete(String idHex) {
        ObjectId id = toObjectId(idHex);
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Freight calculation basis not found for id: " + idHex);
        }
        repository.deleteById(id);
    }
    
    public List<String> getCountriesByProjectId(String projectId) {

        ShipperFreightCalculationBasis basis = repository.findByProjectId(projectId)
            .orElseThrow(() -> new NoSuchElementException("Basis not found"));

        return new ArrayList<>(basis.getCountries().keySet()); 
    }
    
    
    public Object getBasisByCountry(String projectId, String countryCode) {

        ShipperFreightCalculationBasis basis = repository.findByProjectId(projectId)
            .orElseThrow(() -> new NoSuchElementException("Basis not found"));

        Map<String, Object> countriesMap = basis.getCountries();

        // If countryCode not provided → take first country
        if (countryCode == null || countryCode.isEmpty()) {
            countryCode = countriesMap.keySet().stream()
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("No countries configured"));
        }

        Object data = countriesMap.get(countryCode);

        if (data == null) {
            throw new NoSuchElementException("Basis not configured for country: " + countryCode);
        }

        return data;
    }


    
    

    /* ---------- helpers ---------- */

    private ObjectId toObjectId(String idHex) {
        try {
            return new ObjectId(idHex);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ObjectId: " + idHex, e);
        }
    }

    /** Build entity from request DTO. */
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
