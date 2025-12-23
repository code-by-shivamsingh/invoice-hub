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
        if (repository.existsByCompanyId(request.getCompanyId())) {
            throw new Exception("Tolerance limits already exist for CompanyId: " + request.getCompanyId());
        }
        ToleranceLimits entity = mapper.toEntity(request);
        entity.setCompanyId(request.getCompanyId());
        dedupeAncillary(entity);
        ToleranceLimits saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    /** Fetch by CompanyId. 
     * @throws Exception */
    public ToleranceLimitsResponseDTO getByCompanyId(String companyId) throws Exception {
        ToleranceLimits entity = repository.findByCompanyId(companyId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for CompanyId: " + companyId));
        return mapper.toResponse(entity);
    }

    /** Replace entire record for CompanyId. 
     * @throws Exception */
    public ToleranceLimitsResponseDTO replace(String companyId, ToleranceLimitsRequestDTO request) throws Exception {
        ToleranceLimits existing = repository.findByCompanyId(companyId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for CompanyId: " + companyId));

        // Optional: enforce request.CompanyId == path CompanyId
        if (request.getCompanyId() != null && !companyId.equals(request.getCompanyId())) {
            throw new Exception("CompanyId in path and body must match");
        }

        ToleranceLimits updated = mapper.toEntity(request);
        updated.setId(existing.getId());
        updated.setCompanyId(companyId);
        dedupeAncillary(updated);
        return mapper.toResponse(repository.save(updated));
    }

    /** Patch (partial update) for CompanyId. 
     * @throws Exception */
    public ToleranceLimitsResponseDTO patch(String companyId, ToleranceLimitsPatchRequestDTO patch) throws Exception {
        ToleranceLimits entity = repository.findByCompanyId(companyId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for CompanyId: " + companyId));

        if (patch.getCompanyId() != null && !companyId.equals(patch.getCompanyId())) {
            throw new Exception("CompanyId in path and body must match");
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

    /** Delete by CompanyId. 
     * @throws Exception */
    public void deleteByCompanyId(String companyId) throws Exception {
        ToleranceLimits entity = repository.findByCompanyId(companyId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for companyId: " + companyId));
        repository.delete(entity);
    }

    /** Add ancillary tolerance for companyId. 
     * @throws Exception */
    public ToleranceLimitsResponseDTO addAncillary(String companyId, ToleranceDTO dto) throws Exception {
        ToleranceLimits entity = repository.findByCompanyId(companyId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for companyId: " + companyId));
        entity.getAncillaryTolerances().add(mapper.toEntity(dto));
        dedupeAncillary(entity);
        return mapper.toResponse(repository.save(entity));
    }

    /** Remove ancillary tolerance by designation (case-insensitive). 
     * @throws Exception */
    public ToleranceLimitsResponseDTO removeAncillary(String companyId, String designation) throws Exception {
        ToleranceLimits entity = repository.findByCompanyId(companyId)
            .orElseThrow(() -> new Exception("Tolerance limits not found for companyId: " + companyId));
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
