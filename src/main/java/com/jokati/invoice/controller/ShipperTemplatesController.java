
package com.jokati.invoice.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.ShipperTemplateRequestDTO;
import com.jokati.invoice.dto.ShipperTemplateResponseDTO;
import com.jokati.invoice.service.ShipperTemplateService;

import io.swagger.v3.oas.annotations.Operation;
// Avoid importing io.swagger.v3.oas.annotations.responses.ApiResponse due to name collision
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "List returned",
                content = @Content(schema = @Schema(implementation = ShipperTemplateResponseDTO.class))
            )
        }
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShipperTemplateResponseDTO>>> getByUserId(@RequestParam String userId) {
        log.info("Request getByUserId : {}", userId);
        var data = service.findByUserId(userId);
        return ResponseUtil.ok(data, "Templates fetched successfully");
    }

    @Operation(
        summary = "Create a new shipper template",
        description = "Creates a new template with _id = projectId (mirrors Node).",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Created",
                content = @Content(schema = @Schema(implementation = ShipperTemplateResponseDTO.class))
            )
        }
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ShipperTemplateResponseDTO>> create(
            @Valid @RequestBody ShipperTemplateRequestDTO requestDTO) {
        log.info("Request create : {}", requestDTO);
        var saved = service.create(requestDTO);
        // Mirror Node: 200 OK with the created document as data
        return ResponseUtil.ok(saved, "Template created successfully");
    }

    @Operation(summary = "Delete a template by its id (ObjectId hex)")
    @DeleteMapping("/{templateId}")
    public ResponseEntity<ApiResponse<Object>> deleteById(@PathVariable String templateId) {
        log.info("Request deleteById : {}", templateId);
        service.deleteById(templateId);
        return ResponseUtil.okEmpty("Deleted successfully");
    }

    @Operation(summary = "Delete all templates (use with caution)")
    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<Object>> deleteAll() {
        log.info("Request deleteAll");
        long count = service.deleteAll();
        return ResponseUtil.okEmpty("Deleted documents count: " + count);
    }
}
