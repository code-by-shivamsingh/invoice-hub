
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.DieselFloaterResponseDTO;
import com.jokati.invoice.model.DieselFloater;
import com.jokati.invoice.service.DieselFloaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Diesel Floater API
 *
 * Endpoints:
 *  - PUT /api/diesel-floater      : Create or update the matrix (upsert style)
 *  - GET /api/diesel-floater      : Get the current matrix (years map)
 *  - GET /api/diesel-floater/sources : Infer available sources from the first year entry
 */
@RestController
@RequestMapping("/api/diesel-floater")
@Tag(name = "Diesel Floater API", description = "Manage Diesel Floater data (years → sources)")
@Slf4j
public class DieselFloaterController {

    private final DieselFloaterService service;

    public DieselFloaterController(DieselFloaterService service) {
        this.service = service;
    }

    /**
     * Create or update Diesel Floater data.
     * Behavior:
     *  - If no document exists, create a new one (201 Created).
     *  - Else update the first/latest document (200 OK).
     *
     * Expected payload (body):
     * {
     *   "2023": [ { "EN2X": 184.04, "Aral": 185.9, ..., "_id": "..." }, ... ],
     *   "2024": [ ... ],
     *   "2025": [ ... ]
     * }
     *
     * Important: The input is the 'years' map directly, not wrapped in another object.
     */
    @Operation(summary = "Create or update Diesel Floater matrix")
    @PutMapping
    public ResponseEntity<DieselFloaterResponseDTO> createOrUpdate(@RequestBody Map<String, Object> yearsPayload) {
        log.info("DieselFloaterController.createOrUpdate: incoming years payload keys={}", yearsPayload != null ? yearsPayload.keySet() : "null");

        try {
            if (yearsPayload == null || yearsPayload.isEmpty()) {
                log.warn("DieselFloaterController.createOrUpdate: empty body");
                return ResponseEntity.badRequest().body(
                        DieselFloaterResponseDTO.builder().message("Invalid payload: 'years' map required").years(null).build()
                );
            }

            // Use latest if multiple exist (deterministic update behavior)
            Optional<DieselFloater> latestOpt = service.findLatest();
            if (latestOpt.isEmpty()) {
                // Create new
                DieselFloater created = service.save(DieselFloater.builder().years(yearsPayload).build());
                log.info("DieselFloaterController.createOrUpdate: created id={}", created.getId());
                return ResponseEntity.status(201).body(
                        DieselFloaterResponseDTO.builder().message("DieselFloater created").years(yearsPayload).build()
                );
            } else {
                DieselFloater latest = latestOpt.get();
                DieselFloater updated = service.update(latest.getId(),
                        DieselFloater.builder().id(latest.getId()).years(yearsPayload).build());
                log.info("DieselFloaterController.createOrUpdate: updated id={}", updated.getId());
                return ResponseEntity.ok(
                        DieselFloaterResponseDTO.builder().message("DieselFloater updated").years(yearsPayload).build()
                );
            }
        } catch (Exception e) {
            log.error("DieselFloaterController.createOrUpdate: server error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    DieselFloaterResponseDTO.builder().message("Server error: " + e.getMessage()).years(null).build()
            );
        }
    }

    /**
     * Get Diesel Floater matrix ('years' map).
     * Returns 204 No Content if nothing exists.
     */
    @Operation(summary = "Get Diesel Floater matrix (years map)")
    @GetMapping
    public ResponseEntity<DieselFloaterResponseDTO> getMatrix() {
        log.info("DieselFloaterController.getMatrix: request received");

        Optional<DieselFloater> latestOpt = service.findLatest();
        if (latestOpt.isEmpty()) {
            log.warn("DieselFloaterController.getMatrix: no content");
            return ResponseEntity.noContent().build();
        }

        Map<String, Object> years = service.extractYears(latestOpt.get());

        return ResponseEntity.ok(
                DieselFloaterResponseDTO.builder()
                        .message("DieselFloater matrix fetched")
                        .years(years)
                        .build()
        );
    }

    /**
     * Get Diesel Floater sources:
     * - Picks the first (sorted) year and extracts source keys from its first entry,
     * - Filters out "_id".
     * Returns 204 No Content if nothing exists or payload malformed.
     */
    @Operation(summary = "Get Diesel Floater sources (from first year entry)")
    @GetMapping("/sources")
    public ResponseEntity<DieselFloaterResponseDTO> getSources() {
        log.info("DieselFloaterController.getSources: request received");

        Optional<DieselFloater> latestOpt = service.findLatest();
        if (latestOpt.isEmpty()) {
            log.warn("DieselFloaterController.getSources: no content");
            return ResponseEntity.noContent().build();
        }

        Map<String, Object> years = service.extractYears(latestOpt.get());
        List<String> sources = service.extractSources(years);

        if (sources.isEmpty()) {
            log.warn("DieselFloaterController.getSources: sources not found or payload malformed");
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(
                DieselFloaterResponseDTO.builder()
                        .message("DieselFloater sources fetched")
                        .years(sources)
                        .build()
        );
    }
}
