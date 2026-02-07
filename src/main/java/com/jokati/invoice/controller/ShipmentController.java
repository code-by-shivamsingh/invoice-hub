package com.jokati.invoice.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.ShipmentItemRequestDTO;
import com.jokati.invoice.dto.ShipmentRequestDTO;
import com.jokati.invoice.dto.ShipmentSaveResponseDTO;
import com.jokati.invoice.service.ShipmentService;
import com.jokati.invoice.util.TextSanitizer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(
        value = "/api/v1/shipment",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@Tag(name = "Shipment API", description = "Operations related to shipment data")
@Validated
@Slf4j
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService service;

    @Operation(summary = "Get shipment data by projectId")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getShipmentData(@RequestParam @NotBlank String projectId) {
        final String pid = TextSanitizer.normalizeId(projectId);

        log.info("GET /api/v1/shipment projectId={}", pid);

        return service.getByProjectId(pid)
                .map(dto -> ResponseUtil.okObject(dto, "Shipment data fetched successfully"))
                .orElse(ResponseUtil.okEmpty("OK"));
    }

    @Operation(summary = "Save shipment data batch")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> saveShipmentData(@Valid @RequestBody ShipmentRequestDTO request) {

        // NOTE: shipmentId cleanup happens in DTO setters (ShipmentItemRequestDTO),
        // so by the time we reach here it is already sanitized.

        log.info("POST /api/v1/shipment projectId={}, carrierProjectId={}, items={}, append={}",
                request.getProjectId(),
                request.getCarrierProjectId(),
                request.getShipmentData() != null ? request.getShipmentData().size() : 0,
                request.isAppend());

        ShipmentSaveResponseDTO response = service.saveBatch(request);
        return ResponseUtil.okObject(response, "Shipment data saved successfully");
    }
    
    
//    @Operation(summary = "Delete shipment data by projectId")
//    @DeleteMapping(params = "projectId")
//    public ResponseEntity<ApiResponse<Object>> deleteShipmentData(@RequestParam @NotBlank String projectId) {
//        final String pid = TextSanitizer.normalizeId(projectId);
//
//        log.info("DELETE /api/v1/shipment (delete all) projectId={}", pid);
//
//        service.deleteByProjectId(pid);
//        return ResponseUtil.okEmpty("Shipment data deleted successfully");
//    }

    @Operation(summary = "Delete a shipment item by projectId, shipmentId and id")
    @DeleteMapping(params = { "projectId", "shipmentId", "id", "userId" })
    public ResponseEntity<ApiResponse<Object>> deleteShipmentItem(
            @RequestParam @NotBlank String projectId,
            @RequestParam @NotBlank String shipmentId,
            @RequestParam @NotBlank String id,
            @RequestParam @NotBlank String userId) {

        final String pid = TextSanitizer.normalizeId(projectId);
        final String sid = TextSanitizer.normalizeId(shipmentId);
        final String itemId = TextSanitizer.trimUnicode(id);
        final String uid = TextSanitizer.normalizeId(userId);

        log.info("DELETE /api/v1/shipment (delete one item) projectId={}, shipmentId={}, id={}, userId={}",
                pid, sid, itemId, uid);

        ShipmentSaveResponseDTO response = service.deleteShipmentItem(pid, sid, itemId, uid);

        return ResponseUtil.okObject(response, "Shipment item deleted successfully");
    }

    @Operation(summary = "Update shipment item by projectId, shipmentId and id")
    @PutMapping(
            value = "/projects/{projectId}/shipments/{shipmentId}/items/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Object>> updateShipmentItem(
            @PathVariable @NotBlank String projectId,
            @PathVariable @NotBlank String shipmentId,
            @PathVariable @NotBlank String id,
            @Valid @RequestBody ShipmentItemRequestDTO request) {

        final String pid = TextSanitizer.normalizeId(projectId);
        final String sid = TextSanitizer.normalizeId(shipmentId);
        final String itemId = TextSanitizer.trimUnicode(id); // id may be UUID; trim is enough

        log.info("PUT /api/v1/shipment/projects/{}/shipments/{}/items/{}", pid, sid, itemId);

        ShipmentSaveResponseDTO response =
                service.updateShipmentItem(pid, sid, itemId, request);

        return ResponseUtil.okObject(response, "Shipment item updated successfully");
    }

    @Operation(summary = "Get paginated list of shipment items by projectId")
    @GetMapping(value = "/list")
    public ResponseEntity<ApiResponse<Object>> getShipmentList(
            @RequestParam @NotBlank String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        final String pid = TextSanitizer.normalizeId(projectId);

        log.info("GET /api/v1/shipment/list projectId={} page={} size={}", pid, page, size);

        var pagedResult = service.getShipmentList(pid, page, size);

        return ResponseUtil.okObject(pagedResult, "Paginated shipment list fetched successfully");
    }
}