
package com.jokati.invoice.service;

import java.util.List;

import java.math.BigDecimal;
import java.util.Collections;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.jokati.invoice.dto.ToleranceDTO;
import com.jokati.invoice.dto.ToleranceLimitsPatchRequestDTO;
import com.jokati.invoice.dto.ToleranceLimitsRequestDTO;
import com.jokati.invoice.dto.ToleranceLimitsResponseDTO;
import com.jokati.invoice.mapper.ToleranceMapper;
import com.jokati.invoice.model.Tolerance;
import com.jokati.invoice.model.ToleranceLimits;
import com.jokati.invoice.repository.ToleranceLimitsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ToleranceLimitsService {

    private static final Logger log = LoggerFactory.getLogger(ToleranceLimitsService.class);

    private final ToleranceLimitsRepository repository;
    private final ToleranceMapper mapper;

    /** Create a record for a user. Fails if one already exists. */
    public ToleranceLimitsResponseDTO create(ToleranceLimitsRequestDTO request) {
        if (request.getCompanyId() == null || request.getCompanyId().isBlank()) {
            throw new IllegalArgumentException("companyId must not be blank");
        }

        if (repository.existsByCompanyId(request.getCompanyId())) {
            // Prefer DB unique index; if not present, throwing here maps to 409 via global handler
            throw new DuplicateKeyException("Tolerance limits already exist for CompanyId: " + request.getCompanyId());
        }

        ToleranceLimits entity = mapper.toEntity(request);
        entity.setCompanyId(request.getCompanyId());
        dedupeAncillary(entity);

        ToleranceLimits saved = repository.save(entity);
        log.info("Tolerance created: companyId={}", saved.getCompanyId());
        return mapper.toResponse(saved);
    }

    /** Fetch by CompanyId. Throws 404 via GlobalExceptionHandler when not found. */
    /** Fetch by CompanyId. If not found, return default values (0). */
    public ToleranceLimitsResponseDTO getByCompanyId(String companyId) {
        if (!StringUtils.hasText(companyId)) {
            throw new IllegalArgumentException("companyId must not be blank");
        }

        return repository.findByCompanyId(companyId)
                .map(mapper::toResponse)
                .orElseGet(() -> defaultResponse(companyId));
    }

    /** Replace entire record for CompanyId. */
    public ToleranceLimitsResponseDTO replace(String companyId, ToleranceLimitsRequestDTO request) {
        ToleranceLimits existing = repository.findByCompanyId(companyId)
                .orElseThrow(() -> new NoSuchElementException("Tolerance limits not found for CompanyId: " + companyId));

        // Enforce request.companyId == path companyId (if provided)
        if (request.getCompanyId() != null && !companyId.equals(request.getCompanyId())) {
            throw new IllegalArgumentException("CompanyId in path and body must match");
        }

        ToleranceLimits updated = mapper.toEntity(request);
        updated.setId(existing.getId());
        updated.setCompanyId(companyId);
        dedupeAncillary(updated);

        ToleranceLimits saved = repository.save(updated);
        log.info("Tolerance replaced: companyId={}", saved.getCompanyId());
        return mapper.toResponse(saved);
    }

    /** Patch (partial update) for CompanyId. */
    public ToleranceLimitsResponseDTO patch(String companyId, ToleranceLimitsPatchRequestDTO patch) {
        ToleranceLimits entity = repository.findByCompanyId(companyId)
                .orElseThrow(() -> new NoSuchElementException("Tolerance limits not found for CompanyId: " + companyId));

        if (patch.getCompanyId() != null && !companyId.equals(patch.getCompanyId())) {
            throw new IllegalArgumentException("CompanyId in path and body must match");
        }

        mapper.updateEntityFromPatch(patch, entity);

        if (patch.getAncillaryTolerances() != null) {
            List<Tolerance> newList = patch.getAncillaryTolerances()
                    .stream()
                    .map(mapper::toEntity)
                    .toList();
            entity.setAncillaryTolerances(newList);
        }

        dedupeAncillary(entity);
        ToleranceLimits saved = repository.save(entity);
        log.info("Tolerance patched: companyId={}", saved.getCompanyId());
        return mapper.toResponse(saved);
    }

    /** Delete by CompanyId. */
    public void deleteByCompanyId(String companyId) {
        ToleranceLimits entity = repository.findByCompanyId(companyId)
                .orElseThrow(() -> new NoSuchElementException("Tolerance limits not found for CompanyId: " + companyId));
        repository.delete(entity);
        log.info("Tolerance deleted: companyId={}", companyId);
    }

    /** Add ancillary tolerance for companyId. */
    public ToleranceLimitsResponseDTO addAncillary(String companyId, ToleranceDTO dto) {
        ToleranceLimits entity = repository.findByCompanyId(companyId)
                .orElseThrow(() -> new NoSuchElementException("Tolerance limits not found for CompanyId: " + companyId));
        entity.getAncillaryTolerances().add(mapper.toEntity(dto));
        dedupeAncillary(entity);

        ToleranceLimits saved = repository.save(entity);
        log.info("Tolerance ancillary added: companyId={}, designation={}", companyId, dto.getDesignation());
        return mapper.toResponse(saved);
    }

    /** Remove ancillary tolerance by designation (case-insensitive). */
    public ToleranceLimitsResponseDTO removeAncillary(String companyId, String designation) {
        ToleranceLimits entity = repository.findByCompanyId(companyId)
                .orElseThrow(() -> new NoSuchElementException("Tolerance limits not found for CompanyId: " + companyId));

        entity.getAncillaryTolerances().removeIf(a ->
                StringUtils.hasText(designation) && designation.equalsIgnoreCase(a.getDesignation())
        );

        ToleranceLimits saved = repository.save(entity);
        log.info("Tolerance ancillary removed: companyId={}, designation={}", companyId, designation);
        return mapper.toResponse(saved);
    }

    /** Deduplicate ancillary tolerances by lowercased trimmed designation. */
    private void dedupeAncillary(ToleranceLimits entity) {
        entity.setAncillaryTolerances(
                entity.getAncillaryTolerances().stream()
                        .collect(Collectors.toMap(
                                a -> a.getDesignation().toLowerCase().trim(),
                                a -> a,
                                (prev, curr) -> curr
                        ))
                        .values().stream().toList()
        );
    }
    
    private ToleranceLimitsResponseDTO defaultResponse(String companyId) {
        return ToleranceLimitsResponseDTO.builder()
                .id(null)
                .companyId(companyId)
                .freightCostsPercent(BigDecimal.ZERO)
                .standardAdditionalCostsPercent(BigDecimal.ZERO)
                // choose a safe default; adjust if your business wants true
                .onlyPositiveDeviation(Boolean.FALSE)
                // always return an empty list instead of null
                .ancillaryTolerances(Collections.emptyList())
                // no document exists so timestamps are null
                .createdAt(null)
                .updatedAt(null)
                .build();
    }
}
