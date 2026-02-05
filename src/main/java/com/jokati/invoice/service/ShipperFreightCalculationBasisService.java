
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

        Optional<ShipperFreightCalculationBasis> optionalBasis =
                repository.findByProjectId(projectId);

       
        if (optionalBasis.isEmpty()) {
            return List.of("DE");
        }

        ShipperFreightCalculationBasis basis = optionalBasis.get();

        
        if (basis.getCountries() == null || basis.getCountries().isEmpty()) {
            return List.of("DE");
        }

        return new ArrayList<>(basis.getCountries().keySet());
    }

    
    public Object getBasisByCountry(String projectId, String countryCode) {

        Optional<ShipperFreightCalculationBasis> optionalBasis =
                repository.findByProjectId(projectId);

      
        if (optionalBasis.isEmpty()) {
            return null; 
        }
        ShipperFreightCalculationBasis basis = optionalBasis.get();
        Map<String, Object> countriesMap = basis.getCountries();
        if (countriesMap == null || countriesMap.isEmpty()) {
            return null; 
        }
        if (countryCode == null || countryCode.isBlank()) {
            countryCode = countriesMap.keySet().iterator().next();
        }
        Object data = countriesMap.get(countryCode);
        if (data == null) {
            return null; 
        }

                })
                .orElse(null); 
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
