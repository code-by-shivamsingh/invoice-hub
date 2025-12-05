package com.jokati.invoice.controller;

import com.jokati.invoice.dto.ShipperCustomerDataRequestDTO;
import com.jokati.invoice.dto.ShipperCustomerDataResponseDTO;
import com.jokati.invoice.service.ShipperCustomerDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Shipper Customer Data", description = "APIs to manage shipper customer snapshot data")
@RestController
@RequestMapping("/api/v1/shipper-customer-data")
@RequiredArgsConstructor
public class ShipperCustomerDataController {

    private final ShipperCustomerDataService service;

    @Operation(
        summary = "Get latest shipper customer data (first document)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Found",
                content = @Content(schema = @Schema(implementation = ShipperCustomerDataResponseDTO.class)))
        }
    )
    @GetMapping
    public ResponseEntity<?> getLatest() {
        var latest = service.getLatest();
        if (latest.isEmpty()) {
            // Mirror Node behavior: return {} if not found
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(latest.get());
    }

    @Operation(
        summary = "Replace all with a single shipper customer document",
        description = "Deletes all documents and inserts a new one (safe alternative to collection drop).",
        responses = {
            @ApiResponse(responseCode = "201", description = "Created",
                content = @Content(schema = @Schema(implementation = ShipperCustomerDataResponseDTO.class)))
        }
    )
    @PostMapping
    public ResponseEntity<ShipperCustomerDataResponseDTO> replaceAll(@Valid @RequestBody ShipperCustomerDataRequestDTO requestDTO) {
        var saved = service.replaceAll(requestDTO);
        return ResponseEntity.status(201).body(saved);
    }

    @Operation(summary = "Delete all shipper customer data documents")
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAll() {
        long deleted = service.deleteAll();
        return ResponseEntity.ok(Map.of("message", "Deleted documents count: " + deleted));
    }
}
