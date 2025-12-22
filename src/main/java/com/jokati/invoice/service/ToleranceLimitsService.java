package com.jokati.invoice.service;





import com.jokati.invoice.dto.ToleranceDTO;
import com.jokati.invoice.dto.ToleranceLimitsPatchRequestDTO;
import com.jokati.invoice.dto.ToleranceLimitsRequestDTO;
import com.jokati.invoice.dto.ToleranceLimitsResponseDTO;
import com.jokati.invoice.mapper.ToleranceMapper;
import com.jokati.invoice.model.Tolerance;
import com.jokati.invoice.model.ToleranceLimits;
import com.jokati.invoice.repository.ToleranceLimitsRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToleranceLimitsService {


private final ToleranceLimitsRepository repository;
    private final ToleranceMapper mapper;

    /** Create a record for a user. Fails if one already exists. 
     * @throws Exception */
    public ToleranceLimitsResponseDTO create(ToleranceLimitsRequestDTO request) throws Exception {
        if (repository.existsByUserId(request.getUserId())) {
            throw new Exception("Tolerance limits already exist for userId: " + request.getUserId());
        }
        ToleranceLimits entity = mapper.toEntity(request);
        entity.setUserId(request.getUserId());
        dedupeAncillary(entity);
        ToleranceLimits saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    /** Fetch by userId. 
     * @throws Exception */
    public ToleranceLimitsResponseDTO getByUserId(String userId) throws Exception {
        ToleranceLimits entity = repository.findByUserId(userId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for userId: " + userId));
        return mapper.toResponse(entity);
    }

    /** Replace entire record for userId. 
     * @throws Exception */
    public ToleranceLimitsResponseDTO replace(String userId, ToleranceLimitsRequestDTO request) throws Exception {
        ToleranceLimits existing = repository.findByUserId(userId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for userId: " + userId));

        // Optional: enforce request.userId == path userId
        if (request.getUserId() != null && !userId.equals(request.getUserId())) {
            throw new Exception("userId in path and body must match");
        }

        ToleranceLimits updated = mapper.toEntity(request);
        updated.setId(existing.getId());
        updated.setUserId(userId);
        dedupeAncillary(updated);
        return mapper.toResponse(repository.save(updated));
    }

    /** Patch (partial update) for userId. 
     * @throws Exception */
    public ToleranceLimitsResponseDTO patch(String userId, ToleranceLimitsPatchRequestDTO patch) throws Exception {
        ToleranceLimits entity = repository.findByUserId(userId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for userId: " + userId));

        if (patch.getUserId() != null && !userId.equals(patch.getUserId())) {
            throw new Exception("userId in path and body must match");
        }

        mapper.updateEntityFromPatch(patch, entity);

        if (patch.getAncillaryTolerances() != null) {
            List<Tolerance> newList = patch.getAncillaryTolerances().stream()
                .map(mapper::toEntity)
                .toList();
            entity.setAncillaryTolerances(newList);
        }

        dedupeAncillary(entity);
        return mapper.toResponse(repository.save(entity));
    }

    /** Delete by userId. 
     * @throws Exception */
    public void deleteByUserId(String userId) throws Exception {
        ToleranceLimits entity = repository.findByUserId(userId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for userId: " + userId));
        repository.delete(entity);
    }

    /** Add ancillary tolerance for userId. 
     * @throws Exception */
    public ToleranceLimitsResponseDTO addAncillary(String userId, ToleranceDTO dto) throws Exception {
        ToleranceLimits entity = repository.findByUserId(userId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for userId: " + userId));
        entity.getAncillaryTolerances().add(mapper.toEntity(dto));
        dedupeAncillary(entity);
        return mapper.toResponse(repository.save(entity));
    }

    /** Remove ancillary tolerance by designation (case-insensitive). 
     * @throws Exception */
    public ToleranceLimitsResponseDTO removeAncillary(String userId, String designation) throws Exception {
        ToleranceLimits entity = repository.findByUserId(userId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for userId: " + userId));
        entity.getAncillaryTolerances().removeIf(a ->
            StringUtils.hasText(designation) &&
            designation.equalsIgnoreCase(a.getDesignation())
        );
        return mapper.toResponse(repository.save(entity));
    }

    private void dedupeAncillary(ToleranceLimits entity) {
        entity.setAncillaryTolerances(
            entity.getAncillaryTolerances().stream()
                .collect(java.util.stream.Collectors.toMap(
                    a -> a.getDesignation().toLowerCase().trim(),
                    a -> a,
                    (prev, curr) -> curr
                ))
                .values().stream().toList()
        );
    }
}
