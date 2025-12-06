
package com.jokati.invoice.service;

import com.jokati.invoice.dto.ShipperProjectRequestDTO;
import com.jokati.invoice.dto.ShipperProjectResponseDTO;
import com.jokati.invoice.dto.ShipperProjectUpdateRequestDTO;
import com.jokati.invoice.model.ShipperProject;
import com.jokati.invoice.repository.ShipperProjectRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ShipperProjectService {

    private final ShipperProjectRepository repository;

    /** GET all by userId */
    public List<ShipperProjectResponseDTO> findByUserId(String userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /** GET by projectId (_id) */
    public ShipperProjectResponseDTO getByProjectId(String projectIdHex) {
        ObjectId id;
        try {
            id = new ObjectId(projectIdHex);
        } catch (IllegalArgumentException e) {
            return null; // mirror Node: 200 with {}
        }
        var entity = repository.findById(id).orElse(null);
        return entity != null ? toResponseDTO(entity) : null;
    }

    /** POST: create with new ObjectId; validate userId and name via @Valid */
    @Transactional
    public ShipperProjectResponseDTO create(ShipperProjectRequestDTO req) {
        var entity = ShipperProject.builder()
                .id(new ObjectId())
                .userId(req.getUserId())
                .name(req.getName())
                .extra(req.getExtra())
                .build();

        var saved = repository.save(entity);
        return toResponseDTO(saved);
    }

    /** PUT: update by projectId or _id; require name; Node-style error handling */
    @Transactional
    public ShipperProjectResponseDTO update(ShipperProjectUpdateRequestDTO req) {
        String idHex = req.getProjectId() != null && !req.getProjectId().isBlank()
                ? req.getProjectId()
                : req.get_id();

        if (idHex == null || idHex.isBlank()) {
            throw new IllegalArgumentException("Ungültige Projekt-ID");
        }

        ObjectId id;
        try {
            id = new ObjectId(idHex);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ungültige Projekt-ID");
        }

        var existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Projekt nicht gefunden"));

        // Apply updates (follow Node behavior)
        existing.setName(req.getName());
        if (req.getUserId() != null) existing.setUserId(req.getUserId());
        existing.setExtra(req.getExtra());

        var saved = repository.save(existing);
        return toResponseDTO(saved);
    }

    /**
     * DELETE by projectId then return remaining projects for the same user.
     * Mirrors Node handler.
     */
    @Transactional
    public List<ShipperProjectResponseDTO> deleteByProjectId(String projectIdHex) {
        ObjectId id;
        try {
            id = new ObjectId(projectIdHex);
        } catch (IllegalArgumentException e) {
            throw new NoSuchElementException("Projekt nicht gefunden");
        }

        var existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Projekt nicht gefunden"));

        String userId = existing.getUserId();
        repository.deleteById(id);

        return repository.findByUserId(userId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /* -------- mapping -------- */

    private ShipperProjectResponseDTO toResponseDTO(ShipperProject entity) {
        return ShipperProjectResponseDTO.builder()
                .id(entity.getId() != null ? entity.getId().toHexString() : null)
                .userId(entity.getUserId())
                .name(entity.getName())
                .extra(entity.getExtra())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
