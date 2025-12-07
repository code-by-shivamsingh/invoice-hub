
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.ShipperTemplateRequestDTO;
import com.jokati.invoice.dto.ShipperTemplateResponseDTO;
import com.jokati.invoice.service.ShipperTemplateService;
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

import java.util.List;
import java.util.Map;

@Tag(name = "Shipper Templates", description = "APIs to manage shipper templates")
@RestController
@RequestMapping("/api/v1/shipper-templates")
@RequiredArgsConstructor
public class ShipperTemplatesController {
	
	private static final Logger log = LoggerFactory.getLogger(ShipperTemplatesController.class);

    private final ShipperTemplateService service;

    @Operation(
        summary = "Get all templates for a user",
        responses = {
            @ApiResponse(responseCode = "200", description = "List returned",
                content = @Content(schema = @Schema(implementation = ShipperTemplateResponseDTO.class)))
        }
    )
    @GetMapping
    public ResponseEntity<List<ShipperTemplateResponseDTO>> getByUserId(@RequestParam String userId) {
    	log.info("Request getByUserId : {}",  userId);
        return ResponseEntity.ok(service.findByUserId(userId));
    }

    @Operation(
        summary = "Create a new shipper template",
        description = "Creates a new template with _id = projectId (mirrors Node).",
        responses = {
            @ApiResponse(responseCode = "200", description = "Created",
                content = @Content(schema = @Schema(implementation = ShipperTemplateResponseDTO.class)))
        }
    )
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ShipperTemplateRequestDTO requestDTO) {
    	log.info("Request create : {}",  requestDTO);
        var saved = service.create(requestDTO);
        // Mirror Node: on success, return raw doc content (not envelope)
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Delete a template by its id (ObjectId hex)")
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Map<String, Object>> deleteById(@PathVariable String templateId) {
    	log.info("Request deleteById : {}",  templateId);
        service.deleteById(templateId);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
    }

    @Operation(summary = "Delete all templates (use with caution)")
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> deleteAll() {
    	log.info("Request deleteAll");
        long count = service.deleteAll();
        return ResponseEntity.ok(Map.of("message", "Deleted documents count: " + count));
    }
}
