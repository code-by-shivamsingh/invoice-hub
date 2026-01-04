
package com.jokati.invoice.controller;

import org.bson.types.ObjectId;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.service.ShipmentSummaryService;
import com.jokati.invoice.service.ShipmentSummaryService.SummaryInitResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/summary")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Shipment Summary", description = "Endpoints for shipment summary preparation, consolidation, and totals.")
public class ShipmentSummaryController {

    private final ShipmentSummaryService shipmentSummaryService;

    /**
     * GET /api/summary/{projectId}
     * Returns the full summary for the project (prepared+consolidated+totals).
     * Node-style: 200 with {} if invalid/missing.
     */
    @Operation(
        summary = "Get full shipment summary for a project",
        description = "Prepares rows, consolidates by ShipmentId, computes per-country and overall totals."
    )
    @GetMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> getProjectSummary(@PathVariable("projectId") String projectIdHex) {
        log.info("GET /api/summary/{}", projectIdHex);

        SummaryInitResult result = shipmentSummaryService.getSummaryInit(projectIdHex);
        if (result == null) {
            log.warn("No summary result for projectId={}", projectIdHex);
            // Node-style 200 OK with {}
            return ResponseUtil.okEmpty("OK");
        }
        return ResponseUtil.okObject(result, "Summary fetched successfully");
    }

    /**
     * GET /api/summary/{projectId}/shipment/{shipmentId}
     * Returns the summary filtered to a single ShipmentId inside the project.
     * Node-style: 200 with {} if invalid/missing.
     */
    @Operation(
        summary = "Get shipment summary for a specific ShipmentId",
        description = "Filters rows to the given ShipmentId, then prepares, consolidates and totals."
    )
    @GetMapping(value = "/{projectId}/shipment/{shipmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> getSummaryByShipmentId(
            @PathVariable("projectId") String projectIdHex,
            @PathVariable("shipmentId") String shipmentId) {
        log.info("GET /api/summary/{}/shipment/{}", projectIdHex, shipmentId);

        if (shipmentId == null || shipmentId.isBlank()) {
            // Node-style 200 OK with {}
            return ResponseUtil.okEmpty("OK");
        }

        ObjectId projectId;
        try {
            projectId = new ObjectId(projectIdHex);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid ObjectId hex: {}", projectIdHex);
            // Node-style 200 OK with {}
            return ResponseUtil.okEmpty("OK");
        }

        SummaryInitResult result = shipmentSummaryService.getSummaryByShipmentId(projectId, shipmentId);
        if (result == null) {
            log.warn("No filtered summary for projectId={} shipmentId={}", projectIdHex, shipmentId);
            // Node-style 200 OK with {}
            return ResponseUtil.okEmpty("OK");
        }

        return ResponseUtil.okObject(result, "Filtered summary fetched successfully");
    }

    /**
     * Health check / simple ping (uses envelope).
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Health check", description = "Returns 200 OK if controller is reachable.")
    public ResponseEntity<ApiResponse<Object>> health() {
        return ResponseUtil.okObject(
            java.util.Map.of("status", "OK"),
            "Shipment summary service is healthy"
        );
    }
}
