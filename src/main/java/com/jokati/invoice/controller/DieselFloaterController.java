
package com.jokati.invoice.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ErrorCodes;
import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.model.DieselFloater;
import com.jokati.invoice.service.DieselFloaterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Diesel Floater API
 *
 * Endpoints:
 *  - PUT /api/diesel-floater           : Create or update the matrix (upsert style)
 *  - GET /api/diesel-floater           : Get the current matrix (years map)
 *  - GET /api/diesel-floater/sources   : Infer available sources from the first year entry
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
    public ResponseEntity<ApiResponse<Object>> createOrUpdate(@Valid @RequestBody Map<String, Object> yearsPayload) {
        log.info("DieselFloaterController.createOrUpdate: incoming years payload keys={}",
                yearsPayload != null ? yearsPayload.keySet() : "null");

        if (yearsPayload == null || yearsPayload.isEmpty()) {
            log.warn("DieselFloaterController.createOrUpdate: empty body");
            // Standardized error (BAD_REQUEST)
            return ResponseUtil.error(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payload: 'years' map required",
                    new ErrorDetail(ErrorCodes.BAD_REQUEST, "Invalid payload: 'years' map required", "years", null)
            );
        }

        Optional<DieselFloater> latestOpt = service.findLatest();
        if (latestOpt.isEmpty()) {
            DieselFloater created = service.save(DieselFloater.builder().years(yearsPayload).build());
            log.info("DieselFloaterController.createOrUpdate: created id={}", created.getId());
            // 201 Created with data being the years map (Node-like)
            return ResponseUtil.created(yearsPayload, "DieselFloater created");
        } else {
            DieselFloater latest = latestOpt.get();
            DieselFloater updated = service.update(latest.getId(),
                    DieselFloater.builder().id(latest.getId()).years(yearsPayload).build());
            log.info("DieselFloaterController.createOrUpdate: updated id={}", updated.getId());
            return ResponseUtil.ok(yearsPayload, "DieselFloater updated");
        }
    }

    /**
     * Get Diesel Floater matrix ('years' map).
     * Returns 204 No Content if nothing exists.
     */
    @Operation(summary = "Get Diesel Floater matrix (years map)")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getMatrix() {
        log.info("DieselFloaterController.getMatrix: request received");

        Optional<DieselFloater> latestOpt = service.findLatest();
        if (latestOpt.isEmpty()) {
            log.warn("DieselFloaterController.getMatrix: no content");
            return ResponseUtil.noContent("No DieselFloater data");
        }

        Map<String, Object> years = service.extractYears(latestOpt.get());
        return ResponseUtil.ok(years, "DieselFloater matrix fetched");
    }

    /**
     * Get Diesel Floater sources:
     * - Picks the first (sorted) year and extracts source keys from its first entry,
     * - Filters out "_id".
     * Returns 204 No Content if nothing exists or payload malformed.
     */
    @Operation(summary = "Get Diesel Floater sources (from first year entry)")
    @GetMapping("/sources")
    public ResponseEntity<ApiResponse<Object>> getSources() {
        log.info("DieselFloaterController.getSources: request received");

        Optional<DieselFloater> latestOpt = service.findLatest();
        if (latestOpt.isEmpty()) {
            log.warn("DieselFloaterController.getSources: no content");
            return ResponseUtil.noContent("No DieselFloater data");
        }

        Map<String, Object> years = service.extractYears(latestOpt.get());
        List<String> sources = service.extractSources(years);

        if (sources.isEmpty()) {
            log.warn("DieselFloaterController.getSources: sources not found or payload malformed");
            return ResponseUtil.noContent("No sources found");
        }

        // Return sources as the data payload
        return ResponseUtil.ok(sources, "DieselFloater sources fetched");
    }
}
