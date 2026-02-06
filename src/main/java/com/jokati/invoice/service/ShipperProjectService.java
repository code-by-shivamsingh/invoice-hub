package com.jokati.invoice.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.jokati.invoice.dto.ShipperProjectRequestDTO;
import com.jokati.invoice.dto.ShipperProjectResponseDTO;
import com.jokati.invoice.dto.ShipperProjectUpdateRequestDTO;
import com.jokati.invoice.model.ShipperProject;
import com.jokati.invoice.repository.ShipperProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShipperProjectService {

    private final ShipperProjectRepository repository;

    /** GET all by userId */
    public List<ShipperProjectResponseDTO> findByUserId(String userId) {
        return repository.findActiveOrUnsetByUserId(userId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /** GET by projectId (_id) – returns null to mirror Node's {} 200 pattern */
    public ShipperProjectResponseDTO getByProjectId(String projectIdHex) {
        ObjectId id;
        try {
            id = new ObjectId(projectIdHex);
        } catch (IllegalArgumentException e) {
            return null;
        }
        var entity = repository.findById(id).orElse(null);
        return entity != null ? toResponseDTO(entity) : null;
    }

    public ObjectId getProjectIdByCarrier(String userId, String carrierName) throws Exception {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        if (!StringUtils.hasText(carrierName)) {
            throw new IllegalArgumentException("carrierName is required");
        }

        ShipperProject project = repository
                .findFirstByUserIdAndNameIgnoreCase(userId, carrierName)
                .orElseThrow(() -> new Exception(
                        "No shipper project found for userId=" + userId + ", carrierName=" + carrierName));

        return project.getId();
    }

    public String getProjectIdHexByCarrier(String userId, String carrierName) throws Exception {
        ObjectId id = getProjectIdByCarrier(userId, carrierName);
        return id != null ? id.toHexString() : null;
    }

    /**
     * POST: create with new ObjectId
     * ✅ Rule: Do NOT create duplicate with same companyId + name (ignore case)
     * Note: uses your repo method which checks active=true or missing/null.
     */
    @Transactional
    public ShipperProjectResponseDTO create(ShipperProjectRequestDTO req) {
        // Basic validations
        if (!StringUtils.hasText(req.getUserId())) {
            throw new IllegalArgumentException("userId is required");
        }
        if (!StringUtils.hasText(req.getCompanyId())) {
            throw new IllegalArgumentException("companyId is required");
        }
        if (!StringUtils.hasText(req.getName())) {
            throw new IllegalArgumentException("name is required");
        }

        String companyId = req.getCompanyId().trim();
        String name = req.getName().trim();

        // ✅ UPSERT only for ACTIVE projects:
        // If active=true doc exists for same companyId + name (ignore case) => UPDATE it.
        // Else => CREATE new doc.
        ShipperProject entity = repository
                .findFirstActiveByCompanyIdAndNameIgnoreCase(companyId, name)
                .map(existing -> {
                    // ---- UPDATE existing ACTIVE document ----
                    existing.setUserId(req.getUserId());     // keep in sync (if needed)
                    existing.setCompanyId(companyId);        // keep consistent
                    existing.setName(name);

                    existing.setStreet(req.getStreet());
                    existing.setStreetNo(req.getStreetNo());
                    existing.setZipCode(req.getZipCode());
                    existing.setCity(req.getCity());
                    existing.setCountry(req.getCountry());

                    existing.setContactName(req.getContactName());
                    existing.setPhoneNo(req.getPhoneNo());
                    existing.setCustomerNumber(req.getCustomerNumber());
                    existing.setEmail(req.getEmail());

                    // Since rule is "only active=true", keep it active.
                    // If caller sends active, you can choose to honor it; below keeps it true.
                    existing.setActive(true);

                    existing.setExtra(req.getExtra());
                    return existing;
                })
                .orElseGet(() -> {
                    // ---- CREATE new document ----
                    return ShipperProject.builder()
                            .id(new ObjectId())
                            .userId(req.getUserId())
                            .companyId(companyId)
                            .name(name)
                            .street(req.getStreet())
                            .streetNo(req.getStreetNo())
                            .zipCode(req.getZipCode())
                            .city(req.getCity())
                            .country(req.getCountry())
                            .contactName(req.getContactName())
                            .phoneNo(req.getPhoneNo())
                            .customerNumber(req.getCustomerNumber())
                            .email(req.getEmail())
                            // For new document: if req.active is null, default to true (ACTIVE project)
                            .active(req.getActive() != null ? req.getActive() : true)
                            .extra(req.getExtra())
                            .build();
                });

        ShipperProject saved = repository.save(entity);
        return toResponseDTO(saved);
    }

    /**
     * PUT: update by projectId or _id; requires name.
     * ✅ Duplicate rule enforced using EXISTING document's companyId
     * because update DTO does not include companyId.
     */
    @Transactional
    public ShipperProjectResponseDTO update(ShipperProjectUpdateRequestDTO req) {

        String idHex = StringUtils.hasText(req.getProjectId())
                ? req.getProjectId()
                : req.get_id();

        if (!StringUtils.hasText(idHex)) {
            throw new IllegalArgumentException("Invalid project ID");
        }

        ObjectId id;
        try {
            id = new ObjectId(idHex);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid project ID");
        }

        var existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Project not found"));

        if (!StringUtils.hasText(req.getName())) {
            throw new IllegalArgumentException("name is required");
        }

        // New name requested
        String newName = req.getName().trim();

        // ✅ Use companyId from existing document (DTO doesn't have it)
        String companyId = existing.getCompanyId();
        if (!StringUtils.hasText(companyId)) {
            throw new IllegalStateException("Existing project has no companyId; cannot validate duplicates");
        }

        // ✅ Conflict check: same companyId + same name (ignore case), excluding current project
        boolean conflict = repository.findActiveOrUnsetByCompanyIdAndNameIgnoreCase(companyId, newName)
                .stream()
                .anyMatch(p -> p.getId() != null && !p.getId().equals(id));

        if (conflict) {
            throw new IllegalArgumentException(
                    "Shipper project with same name already exists for this companyId");
        }

        // Apply updates
        existing.setName(newName);
        if (req.getUserId() != null) {
            existing.setUserId(req.getUserId());
        }
        existing.setExtra(req.getExtra());

        var saved = repository.save(existing);
        return toResponseDTO(saved);
    }

    /**
     * DELETE by projectId -> deactivate (active=false)
     */
    @Transactional
    public String deleteByProjectId(String projectIdHex) {
        ObjectId id;
        try {
            id = new ObjectId(projectIdHex);
        } catch (IllegalArgumentException e) {
            throw new NoSuchElementException("Project not found");
        }

        var existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Project not found"));

        existing.setActive(false);
        repository.save(existing);

        return "Project deactivated successfully";
    }

    /* -------- mapping -------- */

    private ShipperProjectResponseDTO toResponseDTO(ShipperProject entity) {
        return ShipperProjectResponseDTO.builder()
                .id(entity.getId() != null ? entity.getId().toHexString() : null)
                .userId(entity.getUserId())
                .companyId(entity.getCompanyId())
                .name(entity.getName())
                .street(entity.getStreet())
                .streetNo(entity.getStreetNo())
                .zipCode(entity.getZipCode())
                .city(entity.getCity())
                .country(entity.getCountry())
                .contactName(entity.getContactName())
                .phoneNo(entity.getPhoneNo())
                .customerNumber(entity.getCustomerNumber())
                .email(entity.getEmail())
                .active(entity.getActive())
                .extra(entity.getExtra())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}