
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.ShipperProfileRequestDTO;
import com.jokati.invoice.dto.ShipperProfileResponseDTO;
import com.jokati.invoice.service.ShipperProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Shipper Profile", description = "APIs to manage shipper project profile")
@RestController
@RequestMapping("/api/v1/shipper-profile")
@RequiredArgsConstructor
public class ShipperProfileController {
	private static final Logger log = LoggerFactory.getLogger(ShipperProfileController.class);
    private final ShipperProfileService service;

    @Operation(
        summary = "Get shipper profile by projectId (_id)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Found",
                content = @Content(schema = @Schema(implementation = ShipperProfileResponseDTO.class)))
        }
    )
    @GetMapping
    public ResponseEntity<?> getByProjectId(@RequestParam String projectId) {
    	log.info("Request getByProjectId : {}",  projectId);
        var response = service.getByProjectId(projectId);
        if (response == null) {
            // Mirror Node behavior: return {} if not found
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Create a new shipper profile",
        description = "Creates a new document with _id=projectId.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Created",
                content = @Content(schema = @Schema(implementation = ShipperProfileResponseDTO.class)))
        }
    )
    @PostMapping
    public ResponseEntity<ShipperProfileResponseDTO> create(@Valid @RequestBody ShipperProfileRequestDTO requestDTO) {
    	log.info("Request create : {}",  requestDTO);
    	var saved = service.create(requestDTO);
        return ResponseEntity.ok(saved);
    }

    @Operation(
        summary = "Upsert shipper profile (create or update by projectId)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Updated",
                content = @Content(schema = @Schema(implementation = ShipperProfileResponseDTO.class)))
        }
    )
    @PutMapping
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody ShipperProfileRequestDTO requestDTO) {
    	log.info("Request update : {}",  requestDTO);
    	var saved = service.update(requestDTO);
        return ResponseEntity.ok(Map.of(
                "message", "Sendungsprofil aktualisiert",
                "shipperProfile", saved
        ));
    }

    @Operation(summary = "Delete shipper profile by projectId (_id)")
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteByProjectId(@RequestParam String projectId) {
    	log.info("Request deleteByProjectId : {}",  projectId);
        service.deleteByProjectId(projectId);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
    }

    @Operation(summary = "Delete all shipper profiles")
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> deleteAll() {
    	log.info("Request deleteByProjectId");
        long count = service.deleteAll();
        return ResponseEntity.ok(Map.of("message", "Deleted documents count: " + count));
    }
}
