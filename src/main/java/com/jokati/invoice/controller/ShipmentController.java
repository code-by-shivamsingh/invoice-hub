
package com.jokati.invoice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.ShipmentItemRequestDTO;
import com.jokati.invoice.dto.ShipmentRequestDTO;
import com.jokati.invoice.dto.ShipmentSaveResponseDTO;
import com.jokati.invoice.service.ShipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/shipment")
@Tag(name = "Shipment API", description = "Operations related to shipment data")
@Validated // enable @NotBlank validation on method parameters
public class ShipmentController {

    private static final Logger log = LoggerFactory.getLogger(ShipmentController.class);
    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @Operation(
        summary = "Get shipment data by projectId"
        // If you want envelope schema in Swagger, add fully-qualified @io.swagger.v3.oas.annotations.responses.ApiResponse here
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> getShipmentData(@RequestParam @NotBlank String projectId) {
        log.info("GET /api/shipment projectId={}", projectId);

        return service.getByProjectId(projectId)
                .map(dto -> ResponseUtil.okObject(
                        dto,
                        "Shipment data fetched successfully"
                ))
                // Node-style: 200 OK with {} for missing/invalid
                .orElse(ResponseUtil.okEmpty("OK"));
    }

    @Operation(
        summary = "Save shipment data batch"
        // Add request/response Swagger docs with envelope wrappers if needed
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> saveShipmentData(
            @Valid @RequestBody ShipmentRequestDTO request) {

        log.info("POST /api/shipment projectId={}, carrierProjectId={}, items={}, append={}",
                request.getProjectId(),
                request.getCarrierProjectId(),
                request.getShipmentData() != null ? request.getShipmentData().size() : 0,
                request.isAppend());

        // Validation is handled by @Valid + GlobalExceptionHandler; no manual 400 here
        ShipmentSaveResponseDTO response = service.saveBatch(request);
        return ResponseUtil.okObject(response, "Shipment data saved successfully");
    }

    @Operation(
        summary = "Delete shipment data by projectId"
        // By design here: if not found -> 404 via service (global handler returns envelope)
    )
    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> deleteShipmentData(@RequestParam @NotBlank String projectId) {
        log.info("DELETE /api/shipment projectId={}", projectId);
        service.deleteByProjectId(projectId); // throws NoSuchElementException if not found
        return ResponseUtil.okEmpty("Shipment data deleted successfully");
    }
    
    @Operation(
    	summary = "Update shipment item by projectId, shipmentId and id"
    )
    @PutMapping(
            value = "/projects/{projectId}/shipments/{shipmentId}/items/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Object>> updateShipmentItem(
            @PathVariable @NotBlank String projectId,
            @PathVariable @NotBlank String shipmentId,
            @PathVariable @NotBlank String id,
            @Valid @RequestBody ShipmentItemRequestDTO request) {

        log.info("PUT /api/v1/projects/{}/shipments/{}/items/{}",projectId, shipmentId, id);

        ShipmentSaveResponseDTO response =
                service.updateShipmentItem(projectId, shipmentId, id, request);

        return ResponseUtil.okObject(response,"Shipment item updated successfully");
    }
    
}
