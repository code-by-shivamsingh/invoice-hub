
package com.jokati.invoice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;       // <-- Keep your envelope import
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.ShipperCustomerDataRequestDTO;
import com.jokati.invoice.dto.ShipperCustomerDataResponseDTO;
import com.jokati.invoice.service.ShipperCustomerDataService;

import io.swagger.v3.oas.annotations.Operation;
// DO NOT import: io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Shipper Customer Data", description = "APIs to manage shipper customer snapshot data")
@RestController
@RequestMapping("/api/v1/shipper-customer-data")
@RequiredArgsConstructor
public class ShipperCustomerDataController {
    private static final Logger log = LoggerFactory.getLogger(ShipperCustomerDataController.class);

    private final ShipperCustomerDataService service;

    @Operation(
        summary = "Get latest shipper customer data (first document)",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse( // <-- Fully-qualified
                responseCode = "200",
                description = "Found",
                content = @Content(schema = @Schema(implementation = ShipperCustomerDataResponseDTO.class))
            )
        }
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getLatest() {   // <-- Your envelope type
        log.info("Request getLatest");
        var latest = service.getLatest();
        if (latest.isEmpty()) {
            // Mirror Node behavior: 200 OK with {}
            return ResponseUtil.okEmpty("OK");
        }
        return ResponseUtil.ok(latest.get(), "Shipper customer data fetched successfully");
    }

    @Operation(
        summary = "Replace all with a single shipper customer document",
        description = "Deletes all documents and inserts a new one (safe alternative to collection drop).",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(  // <-- Fully-qualified
                responseCode = "201",
                description = "Created",
                content = @Content(schema = @Schema(implementation = ShipperCustomerDataResponseDTO.class))
            )
        }
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ShipperCustomerDataResponseDTO>> replaceAll(
            @Valid @RequestBody ShipperCustomerDataRequestDTO requestDTO) {
        log.info("Request replaceAll : {}", requestDTO);
        var saved = service.replaceAll(requestDTO);
        return ResponseUtil.created(saved, "Shipper customer data replaced and saved");
    }

    @Operation(summary = "Delete all shipper customer data documents")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Object>> deleteAll() {
        log.info("Request deleteAll");
        long deleted = service.deleteAll();
        // Node-style: return {} + message
        return ResponseUtil.okEmpty("Deleted documents count: " + deleted);
    }
}
