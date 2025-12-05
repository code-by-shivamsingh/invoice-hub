
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.CarrierTemplatesRequestDTO;
import com.jokati.invoice.dto.CarrierTemplatesResponseDTO;
import com.jokati.invoice.model.CarrierTemplates;
import com.jokati.invoice.service.CarrierTemplatesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrier-templates")
@Tag(name = "Carrier Templates API", description = "Manage carrier templates (single document)")
public class CarrierTemplatesController {

    private final CarrierTemplatesService service;

    public CarrierTemplatesController(CarrierTemplatesService service) {
        this.service = service;
    }

    /**
     * GET: find() -> result[0]
     */
    @Operation(summary = "Get the first (current) carrier templates document")
    @GetMapping
    public ResponseEntity<?> getFirst() {
        CarrierTemplates first = service.findFirst();
        if (first == null) {
            // Node responds with result[0]; if empty, you returned undefined implicitly.
            // We'll return 200 with empty object to be safe.
            return ResponseEntity.ok(new Object());
        }
        return ResponseEntity.ok(first);
    }

    /**
     * POST: drop collection then insert new document with generated _id.
     * Mirrors Node logic and returns status, saveResult.
     */
    @Operation(summary = "Replace all templates with a new document (drops collection)")
    @PostMapping
    public ResponseEntity<CarrierTemplatesResponseDTO> replace(@RequestBody CarrierTemplatesRequestDTO request) {
        try {
            CarrierTemplates doc = CarrierTemplates.builder()
                    .id(new ObjectId().toHexString())
                    .template(request.getTemplate())
                    .build();

            CarrierTemplates saved = service.replaceAll(doc);

            return ResponseEntity.status(201).body(CarrierTemplatesResponseDTO.builder()
                    .message("Templates replaced and saved")
                    .data(saved)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(CarrierTemplatesResponseDTO.builder()
                    .message("POST Error: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    /**
     * DELETE: placeholder (Node handler is empty)
     */
    @Operation(summary = "Delete all templates (optional)")
    @DeleteMapping
    public ResponseEntity<CarrierTemplatesResponseDTO> deleteAll() {
        service.deleteAll();
        return ResponseEntity.ok(CarrierTemplatesResponseDTO.builder()
                .message("All templates deleted (if existed)")
                .data(null)
                .build());
    }
}
