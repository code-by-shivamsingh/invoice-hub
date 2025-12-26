
package com.jokati.invoice.controller;

import java.util.Map;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.CarrierTemplatesRequestDTO;
import com.jokati.invoice.model.CarrierTemplates;
import com.jokati.invoice.service.CarrierTemplatesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/carrier-templates")
@Tag(name = "Carrier Templates API", description = "Manage carrier templates (single document)")
public class CarrierTemplatesController {
    private static final Logger log = LoggerFactory.getLogger(CarrierTemplatesController.class);
    private final CarrierTemplatesService service;

    public CarrierTemplatesController(CarrierTemplatesService service) {
        this.service = service;
    }

    /**
     * GET: find() -> result[0]
     */
    @Operation(summary = "Get the first (current) carrier templates document")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getFirst() {
        log.info("Get the first (current) carrier templates document");
        CarrierTemplates first = service.findFirst();
        if (first == null) {
            // Node-style: 200 with empty object
            return ResponseUtil.ok(Map.of(), "OK");
        }
        return ResponseUtil.ok(first, "Carrier templates fetched successfully");
    }

    /**
     * POST: drop collection then insert new document with generated _id.
     * Mirrors Node logic and returns status + saved doc.
     */
    @Operation(summary = "Replace all templates with a new document (drops collection)")
    @PostMapping
    public ResponseEntity<ApiResponse<CarrierTemplates>> replace(@Valid @RequestBody CarrierTemplatesRequestDTO request) {
        log.info("Request replace : {}", request);

        CarrierTemplates doc = CarrierTemplates.builder()
                .id(new ObjectId().toHexString())
                .template(request.getTemplate())
                .build();

        CarrierTemplates saved = service.replaceAll(doc);
        return ResponseUtil.created(saved, "Templates replaced and saved");
    }

    /**
     * DELETE: placeholder (Node handler is empty)
     */
    @Operation(summary = "Delete all templates (optional)")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Object>> deleteAll() {
        log.info("Request deleteAll");
        service.deleteAll();
        return ResponseUtil.ok(Map.of(), "All templates deleted (if existed)");
    }
}
