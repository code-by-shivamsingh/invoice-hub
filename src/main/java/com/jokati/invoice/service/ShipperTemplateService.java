
package com.jokati.invoice.service;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.ShipperTemplateRequestDTO;
import com.jokati.invoice.dto.ShipperTemplateResponseDTO;
import com.jokati.invoice.model.ShipperTemplate;
import com.jokati.invoice.repository.ShipperTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShipperTemplateService {

    private final ShipperTemplateRepository repository;

    public List<ShipperTemplateResponseDTO> findByUserId(String userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Create new template with _id = projectId (mirrors Node POST).
     * Returns the saved document DTO (raw content-like).
     */
    @Transactional
    public ShipperTemplateResponseDTO create(ShipperTemplateRequestDTO req) {
        var id = new ObjectId(req.getProjectId());

        var entity = ShipperTemplate.builder()
                .id(id)
                .userId(req.getUserId())
                .template(req.getTemplate())
                .build();

        var saved = repository.save(entity);
        return toResponseDTO(saved);
    }

    public void deleteById(String templateIdHex) {
        var id = new ObjectId(templateIdHex);
        if (!repository.existsById(id)) {
            // No-op or throw; choose contract. Matching Node (silent): we skip throwing.
            return;
        }
        repository.deleteById(id);
    }

    public long deleteAll() {
        long count = repository.count();
        repository.deleteAll();
        return count;
    }

    /* -------- mapping -------- */

    private ShipperTemplateResponseDTO toResponseDTO(ShipperTemplate entity) {
        return ShipperTemplateResponseDTO.builder()
                .id(entity.getId() != null ? entity.getId().toHexString() : null)
                .userId(entity.getUserId())
                .template(entity.getTemplate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
