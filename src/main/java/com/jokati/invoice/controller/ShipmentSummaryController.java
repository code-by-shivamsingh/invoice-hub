
package com.jokati.invoice.controller;

import com.jokati.invoice.service.ShipmentSummaryService;
import com.jokati.invoice.service.ShipmentSummaryService.SummaryInitResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Shipment Summary REST API
 *
 * Provides endpoints to:
 *  - Fetch project-level shipment summary (prepared + consolidated + totals)
 *  - (Optional) Fetch summary filtered to a specific ShipmentId inside the project
 */
@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Shipment Summary", description = "Endpoints for shipment summary preparation, consolidation, and totals.")
public class ShipmentSummaryController {

    private final ShipmentSummaryService shipmentSummaryService;

    /**
     * GET /api/summary/{projectId}
     *
     * Returns the full summary for the project:
     *  - consolidated rows grouped by country
     *  - per-country totals
     *  - overall total shipment price
     */
    @GetMapping("/{projectId}")
    @Operation(
        summary = "Get full shipment summary for a project",
        description = "Prepares rows, consolidates by ShipmentId (if applicable), computes per-country and overall totals.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Summary returned"),
            @ApiResponse(responseCode = "400", description = "Invalid projectId"),
            @ApiResponse(responseCode = "404", description = "Project not found or no data"),
            @ApiResponse(responseCode = "500", description = "Server error")
        }
    )
    public ResponseEntity<SummaryInitResult> getProjectSummary(
            @Parameter(description = "MongoDB ObjectId hex string of the project", example = "692af31934df801237c8fdda")
            @PathVariable("projectId") String projectIdHex
    ) {
        log.info("GET /api/summary/{}", projectIdHex);


        SummaryInitResult result = shipmentSummaryService.getSummaryInit(projectIdHex);
        if (result == null) {
            log.warn("No summary result for projectId={}", projectIdHex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/summary/{projectId}/shipment/{shipmentId}
     *
     * Returns the summary **filtered to a single ShipmentId** inside the project.
     * Useful when a ShipmentId appears multiple times and you want totals for only that group.
     *
     * Note: This method expects that ShipmentSummaryService has a corresponding method.
     * If not yet implemented, add one similar to getSummaryInit but filter rows before preparing:
     *   - fetch project rows
     *   - filter rows where row.getShipmentId().equals(shipmentId)
     *   - then run prepare -> consolidate -> totals pipeline
     */
    @GetMapping("/{projectId}/shipment/{shipmentId}")
    @Operation(
        summary = "Get shipment summary for a specific ShipmentId",
        description = "Filters rows to the given ShipmentId, then prepares, consolidates and totals.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Filtered summary returned"),
            @ApiResponse(responseCode = "400", description = "Invalid projectId or shipmentId"),
            @ApiResponse(responseCode = "404", description = "Project or shipment not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
        }
    )
    public ResponseEntity<SummaryInitResult> getSummaryByShipmentId(
            @Parameter(description = "MongoDB ObjectId hex string of the project", example = "692af31934df801237c8fdda")
            @PathVariable("projectId") String projectIdHex,
            @Parameter(description = "ShipmentId within the project", example = "39134034331")
            @PathVariable("shipmentId") @Schema(minLength = 1) String shipmentId
    ) {
        log.info("GET /api/summary/{}/shipment/{}", projectIdHex, shipmentId);

        if (shipmentId == null || shipmentId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        ObjectId projectId;
        try {
            projectId = new ObjectId(projectIdHex);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid ObjectId: {}", projectIdHex, ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // If you have method implemented in service:
        // SummaryInitResult result = shipmentSummaryService.getSummaryByShipmentId(projectId, shipmentId);

        // Until then, you could simulate by calling getSummaryInit and letting frontend filter, but better add a service method.
        SummaryInitResult result = null; // TODO: implement and replace

        if (result == null) {
            log.warn("No filtered summary for projectId={} shipmentId={}", projectIdHex, shipmentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Health check / simple ping
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns 200 OK if controller is reachable.")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
