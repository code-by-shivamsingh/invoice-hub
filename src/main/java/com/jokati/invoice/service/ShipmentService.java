
package com.jokati.invoice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.ShipmentItemResponseDTO;
import com.jokati.invoice.dto.ShipmentRequestDTO;
import com.jokati.invoice.dto.ShipmentSaveResponseDTO;
import com.jokati.invoice.mapper.ShipmentMapper;
import com.jokati.invoice.model.ProjectShipmentDocument;
import com.jokati.invoice.model.ShipmentItemDocument;
import com.jokati.invoice.repository.ProjectShipmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ProjectShipmentRepository repository;

    public Optional<ShipmentSaveResponseDTO> getByProjectId(String projectId) {
        return repository.findByProjectId(projectId)
                .map(ProjectShipmentDocument::getShipmentData)
                .map(list -> list == null ? List.<ShipmentItemDocument>of() : list)
                .map(list -> list.stream().map(ShipmentMapper::toResponse).collect(Collectors.toList()))
                .map(resList -> ShipmentSaveResponseDTO.builder().shipmentData(resList).build());
    }

    @Transactional
    public ShipmentSaveResponseDTO saveBatch(ShipmentRequestDTO request) {
        ProjectShipmentDocument agg = repository.findByProjectId(request.getProjectId())
                .orElse(ProjectShipmentDocument.builder()
                        .projectId(request.getProjectId())
                        .carrierProjectId(request.getCarrierProjectId())
                        .createdAt(Instant.now())
                        .shipmentData(new ArrayList<>())
                        .build());

        // Always update carrierProjectId from request if provided
        if (request.getCarrierProjectId() != null && !request.getCarrierProjectId().isBlank()) {
            agg.setCarrierProjectId(request.getCarrierProjectId());
        }

        List<ShipmentItemDocument> incoming = request.getShipmentData().stream()
                .map(ShipmentMapper::toDocument)
                .toList();

        if (request.isAppend()) {
            List<ShipmentItemDocument> current = agg.getShipmentData() != null ? agg.getShipmentData() : new ArrayList<>();
            current.addAll(incoming);
            agg.setShipmentData(current);
        } else {
            agg.setShipmentData(incoming);
        }

        if (agg.getCreatedAt() == null) agg.setCreatedAt(Instant.now());
        repository.save(agg);

        List<ShipmentItemResponseDTO> responseItems = agg.getShipmentData().stream()
                .map(ShipmentMapper::toResponse)
                .toList();

        return ShipmentSaveResponseDTO.builder()
                .shipmentData(responseItems)
                .build();
    }

    @Transactional
    public void deleteByProjectId(String projectId) {
        Optional<ProjectShipmentDocument> existing = repository.findByProjectId(projectId);
        if (existing.isEmpty()) {
            throw new java.util.NoSuchElementException("Shipment data not found for projectId: " + projectId);
        }
        repository.deleteByProjectId(projectId);
    }

    public List<ShipmentItemDocument> getShipmentsByShipmentIdAndProjectId(ObjectId projectId, String shipmentId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        if (shipmentId == null || shipmentId.isBlank()) {
            throw new IllegalArgumentException("shipmentId must not be null or blank");
        }

        String projectIdStr = projectId.toHexString();

        return repository.findByProjectId(projectIdStr)
                .map(ProjectShipmentDocument::getShipmentData)
                .filter(Objects::nonNull)
                .orElseGet(List::of)
                .stream()
                .filter(item -> shipmentId.equals(item.getShipmentId()))
                .collect(Collectors.toList());
    }
}
