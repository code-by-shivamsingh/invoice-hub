package com.jokati.invoice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.ShipmentItemRequestDTO;
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
		return repository.findByProjectId(projectId).map(ProjectShipmentDocument::getShipmentData)
				.map(list -> list == null ? List.<ShipmentItemDocument>of() : list)
				.map(list -> list.stream().map(ShipmentMapper::toResponse).collect(Collectors.toList()))
				.map(resList -> ShipmentSaveResponseDTO.builder().shipmentData(resList).build());
	}

	public Map<String, Object> getShipmentList(String projectId, int page, int size) {

		if (page < 0) {
			throw new IllegalArgumentException("page must be >= 0");
		}
		if (size <= 0) {
			throw new IllegalArgumentException("size must be > 0");
		}

		List<ShipmentItemDocument> shipmentData = repository.findByProjectId(projectId)
				.map(ProjectShipmentDocument::getShipmentData).orElse(Collections.emptyList());

		int totalItems = shipmentData.size();
		int totalPages = (int) Math.ceil((double) totalItems / size);

		int fromIndex = page * size;

		if (fromIndex >= totalItems) {
			fromIndex = totalItems;
		}

		int toIndex = Math.min(fromIndex + size, totalItems);

		List<ShipmentItemResponseDTO> pagedShipments = shipmentData.subList(fromIndex, toIndex).stream()
				.map(ShipmentMapper::toResponse).toList();

		Map<String, Object> response = new HashMap<>();
		response.put("shipments", pagedShipments);
		response.put("currentPage", page);
		response.put("pageSize", size);
		response.put("totalItems", totalItems);
		response.put("totalPages", totalPages);

		return response;
	}

	@Transactional
	public ShipmentSaveResponseDTO saveBatch(ShipmentRequestDTO request) {

		ProjectShipmentDocument agg = repository.findByProjectId(request.getProjectId())
				.orElse(ProjectShipmentDocument.builder().projectId(request.getProjectId())
						.carrierProjectId(request.getCarrierProjectId()).createdAt(Instant.now())
						.shipmentData(new ArrayList<>()).build());

		if (request.getCarrierProjectId() != null && !request.getCarrierProjectId().isBlank()) {
			agg.setCarrierProjectId(request.getCarrierProjectId());
		}

		List<ShipmentItemDocument> incoming = request.getShipmentData().stream().map(ShipmentMapper::toDocument)
				.toList();

		if (request.isAppend()) {
			List<ShipmentItemDocument> current = agg.getShipmentData() != null ? agg.getShipmentData()
					: new ArrayList<>();
			current.addAll(incoming);
			agg.setShipmentData(current);
		} else {
			agg.setShipmentData(incoming);
		}

		if (agg.getCreatedAt() == null) {
			agg.setCreatedAt(Instant.now());
		}

		repository.save(agg);

		List<ShipmentItemResponseDTO> responseItems = agg.getShipmentData().stream().map(ShipmentMapper::toResponse)
				.toList();

		return ShipmentSaveResponseDTO.builder().shipmentData(responseItems).build();
	}

	@Transactional
	public void deleteByProjectId(String projectId) {
		repository.findByProjectId(projectId).orElseThrow(
				() -> new java.util.NoSuchElementException("Shipment data not found for projectId: " + projectId));
		repository.deleteByProjectId(projectId);
	}

	public List<ShipmentItemDocument> getShipmentsByShipmentIdAndProjectId(ObjectId projectId, String shipmentId) {

		String projectIdStr = projectId.toHexString();

		return repository.findByProjectId(projectIdStr).map(ProjectShipmentDocument::getShipmentData)
				.orElseGet(List::of).stream().filter(item -> shipmentId.equals(item.getShipmentId()))
				.collect(Collectors.toList());
	}

	@Transactional
	public ShipmentSaveResponseDTO updateShipmentItem(String projectId, String shipmentId, String id,
			ShipmentItemRequestDTO request) {

		ProjectShipmentDocument agg = repository.findByProjectId(projectId).orElseThrow(
				() -> new java.util.NoSuchElementException("Shipment data not found for projectId: " + projectId));

		boolean updated = false;

		for (ShipmentItemDocument item : agg.getShipmentData()) {

			boolean idMatched = id.equals(item.getIdUpper()) || id.equals(item.getIdLower());

			if (shipmentId.equals(item.getShipmentId()) && idMatched) {
				ShipmentMapper.updateDocument(item, request);
				updated = true;
				break;
			}
		}

		if (!updated) {
			throw new java.util.NoSuchElementException(
					"Shipment item not found for shipmentId: " + shipmentId + " and id: " + id);
		}

		repository.save(agg);

		List<ShipmentItemResponseDTO> responseItems = agg.getShipmentData().stream().map(ShipmentMapper::toResponse)
				.toList();

		return ShipmentSaveResponseDTO.builder().shipmentData(responseItems).build();
	}
}
