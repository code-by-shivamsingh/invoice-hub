
package com.jokati.invoice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.ShipperRatesRequestDTO;
import com.jokati.invoice.dto.ShipperRatesResponseDTO;
import com.jokati.invoice.model.ShipperRates;
import com.jokati.invoice.service.ShipperRatesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shipper-rates")
@Tag(name = "Shipper Rates API", description = "Manage shipper rates")
public class ShipperRatesController {
    private static final Logger log = LoggerFactory.getLogger(ShipperRatesController.class);

    private final ShipperRatesService service;

    public ShipperRatesController(ShipperRatesService service) {
        this.service = service;
    }

    @Operation(summary = "Create shipper rates")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(@Valid @RequestBody ShipperRatesRequestDTO request) {
        log.info("Request create : {}", request);

        ShipperRates entity = toEntity(request);
        ShipperRates saved = service.save(entity);

        return ResponseUtil.okObject(
                toResponseDTO(saved),
                "Shipper rates saved successfully"
        );
    }

    @Operation(summary = "Update shipper rates")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable String id,
                                                      @Valid @RequestBody ShipperRatesRequestDTO request) {
        log.info("Request update for id : {} and request {}", id, request);

        ShipperRates entity = toEntity(request);
        ShipperRates updated = service.update(id, entity);

        return ResponseUtil.okObject(
                toResponseDTO(updated),
                "Shipper rates updated successfully"
        );
    }

    @Operation(summary = "Get shipper rates by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getShipperRatesById(@PathVariable String id) {
        log.info("Request getShipperRatesById : {}", id);

        return service.findById(id)
                .map(rates -> ResponseUtil.okObject(
                        toResponseDTO(rates),
                        "Shipper rates fetched successfully"
                ))
                // Node-style: 200 OK with {} when missing/invalid
                .orElse(ResponseUtil.okEmpty("OK"));
    }

    @Operation(summary = "Delete shipper rates by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteShipperRatesById(@PathVariable String id) {
        log.info("Request deleteShipperRatesById : {}", id);
        service.delete(id);
        // Envelope message carries the deletion message; data = {}
        return ResponseUtil.okEmpty("Shipper rates deleted successfully");
    }

    /* ---------- mapping helpers ---------- */

    private ShipperRates toEntity(ShipperRatesRequestDTO request) {
        return ShipperRates.builder()
                .projectId(request.getProjectId())
                .rates(request.getRates())
                .build();
    }


	private ShipperRatesResponseDTO toResponseDTO(ShipperRates entity) {
	    return ShipperRatesResponseDTO.builder()
	            .id(entity.getId()) // If ObjectId, use entity.getId().toHexString()
	            .projectId(entity.getProjectId())
	            .rates(entity.getRates())
	            .build();
	}

}
