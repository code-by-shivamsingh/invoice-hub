
package com.jokati.invoice.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.ShipperFreightCalculationBasisRequestDTO;
import com.jokati.invoice.dto.ShipperFreightCalculationBasisResponseDTO;
import com.jokati.invoice.model.ShipperFreightCalculationBasis;
import com.jokati.invoice.repository.ShipperFreightCalculationBasisRepository;

import lombok.RequiredArgsConstructor;

import org.bson.types.ObjectId;
import org.springframework.util.StringUtils;

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

    
//    public Object getBasisByCountry(String projectId, String countryCode) {
//
//        Optional<ShipperFreightCalculationBasis> optionalBasis =
//                repository.findByProjectId(projectId);
//
//        if (optionalBasis.isEmpty()) {
//            return null;
//        }
//
//        ShipperFreightCalculationBasis basis = optionalBasis.get();
//        Map<String, Object> countriesMap = basis.getCountries();
//
//        if (countriesMap == null || countriesMap.isEmpty()) {
//            return null;
//        }
//
//        if (countryCode == null || countryCode.isBlank()) {
//            countryCode = countriesMap.keySet().iterator().next();
//        }
//
//        return countriesMap.get(countryCode); 
//    }
    

public ShipperFreightCalculationBasis getBasisByCountry(String projectId, String countryCode) {

    Optional<ShipperFreightCalculationBasis> optionalBasis = repository.findByProjectId(projectId);

    if (optionalBasis.isEmpty()) {
        return null; // projectId not found
    }

    ShipperFreightCalculationBasis basis = optionalBasis.get();
    Map<String, Object> countriesMap = basis.getCountries();

    if (countriesMap == null || countriesMap.isEmpty()) {
        // project exists but has no countries -> return payload with countries null
        return copyWithCountries(basis, null);
    }

    // if countryCode is missing, you can keep previous behavior:
    // either pick first OR return full list OR return null countries.
    // I'll keep your earlier expectation: require explicit, otherwise pick first.
    if (countryCode == null || countryCode.isBlank()) {
        // choose first available country
        String firstKey = countriesMap.keySet().iterator().next();
        Map<String, Object> filtered = new LinkedHashMap<>();
        filtered.put(firstKey, countriesMap.get(firstKey));
        return copyWithCountries(basis, filtered);
    }

    // Find actual key ignoring case
    String actualKey = countriesMap.keySet().stream()
            .filter(k -> k != null && k.equalsIgnoreCase(countryCode.trim()))
            .findFirst()
            .orElse(null);

    if (actualKey == null) {
        // ✅ project exists but requested country not found -> return payload with countries null
        return copyWithCountries(basis, null);
    }

    // Filter to requested country only
    Map<String, Object> filteredCountries = new LinkedHashMap<>();
    filteredCountries.put(actualKey, countriesMap.get(actualKey));

    return copyWithCountries(basis, filteredCountries);
}

private ShipperFreightCalculationBasis copyWithCountries(ShipperFreightCalculationBasis basis,
                                                        Map<String, Object> countries) {
    ShipperFreightCalculationBasis response = new ShipperFreightCalculationBasis();
    response.setId(basis.getId());
    response.setProjectId(basis.getProjectId());
    response.setCarrierProjectId(basis.getCarrierProjectId());
    response.setCountries(countries); // ✅ null when not found
    response.setFirebaseId(basis.getFirebaseId());
    response.setExtra(basis.getExtra());
    response.setCreatedAt(basis.getCreatedAt());
    response.setUpdatedAt(basis.getUpdatedAt());
    return response;
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
//    public ShipperFreightCalculationBasis fromRequestDTO(ShipperFreightCalculationBasisRequestDTO req) {
//        return ShipperFreightCalculationBasis.builder()
//        		
//                .projectId(req.getProjectId())
//                .carrierProjectId(req.getCarrierProjectId())
//                .countries(req.getCountries())
//                .firebaseId(req.getFirebaseId())
//                .extra(req.getExtra())
//                .build();
//    }
    

public ShipperFreightCalculationBasis fromRequestDTO(ShipperFreightCalculationBasisRequestDTO req) {

    ObjectId objectId = null;

    // ✅ If client sends id, use it for update
    if (StringUtils.hasText(req.getId())) {
        try {
            objectId = new ObjectId(req.getId().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid id format. Must be a valid Mongo ObjectId hex string.");
        }
    }

    return ShipperFreightCalculationBasis.builder()
            .id(objectId) // ✅ this is what makes repository.save() update instead of insert
            .projectId(req.getProjectId())
            .carrierProjectId(req.getCarrierProjectId())
            .countries(req.getCountries())
            .firebaseId(req.getFirebaseId())
            .extra(req.getExtra())
            .build();
}

@Transactional
public ShipperFreightCalculationBasis createOrUpdateCountryWise(ShipperFreightCalculationBasisRequestDTO request) {

    if (!StringUtils.hasText(request.getProjectId())) {
        throw new IllegalArgumentException("projectId is required");
    }
    if (request.getCountries() == null || request.getCountries().isEmpty()) {
        throw new IllegalArgumentException("countries must not be empty");
    }

    // Parse id if present
    ObjectId objectId = null;
    if (StringUtils.hasText(request.getId())) {
        try {
            objectId = new ObjectId(request.getId().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid id format. Must be Mongo ObjectId hex string.");
        }
    }

    // Load existing doc: by id (preferred), else by projectId
    ShipperFreightCalculationBasis basis = null;

    if (objectId != null) {
        basis = repository.findById(objectId).orElse(null);
    }
    if (basis == null) {
        basis = repository.findByProjectId(request.getProjectId()).orElse(null);
    }

    // If not found -> create new
    if (basis == null) {
        ShipperFreightCalculationBasis newEntity = fromRequestDTO(request);
        if (newEntity.getId() == null) {
            newEntity.setId(new ObjectId());
        }
        // Ensure at least active country payload is stored
        return repository.save(newEntity);
    }

    // Merge countries safely (case-insensitive replacement)
    Map<String, Object> existingCountries = basis.getCountries();
    if (existingCountries == null) {
        existingCountries = new LinkedHashMap<>();
    }

    // For each incoming country code: remove any existing key that matches ignoring case, then put new
    for (Map.Entry<String, Object> entry : request.getCountries().entrySet()) {
        String incomingCode = entry.getKey();
        if (!StringUtils.hasText(incomingCode)) {
            continue;
        }

        // remove old key if matches ignoring case (avoid DE + de duplicates)
        String oldKey = existingCountries.keySet().stream()
                .filter(k -> k != null && k.equalsIgnoreCase(incomingCode.trim()))
                .findFirst()
                .orElse(null);

        if (oldKey != null) {
            existingCountries.remove(oldKey);
        }

        existingCountries.put(incomingCode.trim(), entry.getValue());
    }

    basis.setCountries(existingCountries);

    // Update other fields (safe)
    basis.setProjectId(request.getProjectId());
    if (request.getCarrierProjectId() != null) {
        basis.setCarrierProjectId(request.getCarrierProjectId());
    }
    if (request.getFirebaseId() != null) {
        basis.setFirebaseId(request.getFirebaseId());
    }
    if (request.getExtra() != null) {
        basis.setExtra(request.getExtra());
    }

    return repository.save(basis);
}

public Map<String, Object> filterCountriesForResponse(ShipperFreightCalculationBasis saved,
        Map<String, Object> requestCountries) {

Map<String, Object> savedCountries = saved.getCountries();
if (savedCountries == null || savedCountries.isEmpty() || requestCountries == null || requestCountries.isEmpty()) {
return new LinkedHashMap<>();
}

Map<String, Object> filtered = new LinkedHashMap<>();

for (String reqKey : requestCountries.keySet()) {
if (!StringUtils.hasText(reqKey)) continue;

// Find actual key in saved map ignoring case
String actualKey = savedCountries.keySet().stream()
.filter(k -> k != null && k.equalsIgnoreCase(reqKey.trim()))
.findFirst()
.orElse(null);

if (actualKey != null) {
filtered.put(actualKey, savedCountries.get(actualKey));
}
}

return filtered;
}


}
