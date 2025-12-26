
package com.jokati.invoice.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.ShipperProfileRequestDTO;
import com.jokati.invoice.dto.ShipperProfileResponseDTO;
import com.jokati.invoice.service.ShipperProfileService;

import io.swagger.v3.oas.annotations.Operation;
// DO NOT import: io.swagger.v3.oas.annotations.responses.ApiResponse (name collision)
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
            @io.swagger.v3.oas.annotations.responses.ApiResponse( // use fully-qualified to avoid collision
                responseCode = "200",
                description = "Found",
                content = @Content(schema = @Schema(implementation = ShipperProfileResponseDTO.class))
            )
        }
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getByProjectId(@RequestParam String projectId) {
        log.info("Request getByProjectId : {}", projectId);
        var response = service.getByProjectId(projectId);
        if (response == null) {
            // Mirror Node behavior: return {} if not found
            return ResponseUtil.okEmpty("OK");
        }
        return ResponseUtil.ok(response, "Shipper profile fetched successfully");
    }

    @Operation(
        summary = "Create a new shipper profile",
        description = "Creates a new document with _id=projectId.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Created",
                content = @Content(schema = @Schema(implementation = ShipperProfileResponseDTO.class))
            )
        }
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ShipperProfileResponseDTO>> create(
            @Valid @RequestBody ShipperProfileRequestDTO requestDTO) {
        log.info("Request create : {}", requestDTO);
        var saved = service.create(requestDTO);
        // Node used 200 OK for creation; keep same to mirror behavior.
        return ResponseUtil.ok(saved, "Shipper profile created successfully");
    }

    @Operation(
        summary = "Upsert shipper profile (create or update by projectId)",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Updated",
                content = @Content(schema = @Schema(implementation = ShipperProfileResponseDTO.class))
            )
        }
    )
    @PutMapping
    public ResponseEntity<ApiResponse<Object>> update(@Valid @RequestBody ShipperProfileRequestDTO requestDTO) {
        log.info("Request update : {}", requestDTO);
        var saved = service.update(requestDTO);
        // Node-style: message + object; we use envelope with message in body and data=object
        return ResponseUtil.ok(
            Map.of(
                "message", "Sendungsprofil aktualisiert",
                "shipperProfile", saved
            ),
            "Sendungsprofil aktualisiert"
        );
    }

    @Operation(summary = "Delete shipper profile by projectId (_id)")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Object>> deleteByProjectId(@RequestParam String projectId) {
        log.info("Request deleteByProjectId : {}", projectId);
        service.deleteByProjectId(projectId);
        return ResponseUtil.okEmpty("Deleted successfully");
    }

    @Operation(summary = "Delete all shipper profiles")
    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<Object>> deleteAll() {
        log.info("Request deleteAll");
        long count = service.deleteAll();
        return ResponseUtil.okEmpty("Deleted documents count: " + count);
    }
}
