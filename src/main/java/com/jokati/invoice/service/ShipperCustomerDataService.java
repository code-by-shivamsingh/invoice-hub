
package com.jokati.invoice.service;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.ShipperCustomerDataRequestDTO;
import com.jokati.invoice.dto.ShipperCustomerDataResponseDTO;
import com.jokati.invoice.model.ShipperCustomerData;
import com.jokati.invoice.repository.ShipperCustomerDataRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShipperCustomerDataService {

    private final ShipperCustomerDataRepository repository;

    /**
     * Mirrors Node GET: return the first/latest document.
     */
    public Optional<ShipperCustomerDataResponseDTO> getLatest() {
        return repository.findTopByOrderByUpdatedAtDesc()
                .map(this::toResponseDTO);
    }

    /**
     * Mirrors Node POST: drop collection then insert one document.
     * Implemented as deleteAll + insert to retain indexes and avoid race conditions.
     */
    @Transactional
    public ShipperCustomerDataResponseDTO replaceAll(ShipperCustomerDataRequestDTO requestDTO) {
        repository.deleteAll();

        var entity = ShipperCustomerData.builder()
                .id(new ObjectId())
                .data(requestDTO.getData())
                .build();

        var saved = repository.save(entity);
        return toResponseDTO(saved);
    }

    @Transactional
    public long deleteAll() {
        long count = repository.count();
        repository.deleteAll();
        return count;
    }

    /* -------- mapping -------- */

    private ShipperCustomerDataResponseDTO toResponseDTO(ShipperCustomerData entity) {
        return ShipperCustomerDataResponseDTO.builder()
                .id(entity.getId() != null ? entity.getId().toHexString() : null)
                .data(entity.getData())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
