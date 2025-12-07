
package com.jokati.invoice.service;

import com.jokati.invoice.dto.ShipperProfileRequestDTO;
import com.jokati.invoice.dto.ShipperProfileResponseDTO;
import com.jokati.invoice.model.ShipperProfile;
import com.jokati.invoice.repository.ShipperProfileRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ShipperProfileService {

    private final ShipperProfileRepository repository;

    public ShipperProfileResponseDTO getByProjectId(String projectIdHex) {
        var id = new ObjectId(projectIdHex);
        var entity = repository.findById(id).orElse(null);
        return entity != null ? toResponseDTO(entity) : null;
    }

    /**
     * Create new document with _id = projectId (mirrors Node POST).
     */
    @Transactional
    public ShipperProfileResponseDTO create(ShipperProfileRequestDTO req) {
        var id = new ObjectId(req.getProjectId());
        var entity = ShipperProfile.builder()
                .id(id)
                .profile(req.getProfile())
                .build();

        var saved = repository.save(entity);
        return toResponseDTO(saved);
    }

    /**
     * Upsert by _id = projectId (mirrors Node PUT findByIdAndUpdate(..., { upsert: true })).
     */
    @Transactional
    public ShipperProfileResponseDTO update(ShipperProfileRequestDTO req) {
        var id = new ObjectId(req.getProjectId());
        var entity = repository.findById(id).orElse(
                ShipperProfile.builder().id(id).build()
        );

        entity.setProfile(req.getProfile());

        var saved = repository.save(entity);
        return toResponseDTO(saved);
    }

    public void deleteByProjectId(String projectIdHex) {
        var id = new ObjectId(projectIdHex);
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Document not found for projectId=" + projectIdHex);
        }
        repository.deleteById(id);
    }

    public long deleteAll() {
        long count = repository.count();
        repository.deleteAll();
        return count;
    }

    /* -------- mapping -------- */

    private ShipperProfileResponseDTO toResponseDTO(ShipperProfile entity) {
        return ShipperProfileResponseDTO.builder()
                .id(entity.getId() != null ? entity.getId().toHexString() : null)
                .profile(entity.getProfile())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
