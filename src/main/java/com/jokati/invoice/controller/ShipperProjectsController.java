
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.ShipperProjectRequestDTO;
import com.jokati.invoice.dto.ShipperProjectResponseDTO;
import com.jokati.invoice.dto.ShipperProjectUpdateRequestDTO;
import com.jokati.invoice.service.ShipperProjectService;
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

@Tag(name = "Shipper Projects", description = "APIs to manage shipper projects")
@RestController
@RequestMapping("/api/v1/shipper-projects")
@RequiredArgsConstructor
public class ShipperProjectsController {
	private static final Logger log = LoggerFactory.getLogger(ShipperProjectsController.class);

    private final ShipperProjectService service;

    @Operation(
        summary = "Get all projects for a user",
        responses = {
            @ApiResponse(responseCode = "200", description = "List returned",
                content = @Content(schema = @Schema(implementation = ShipperProjectResponseDTO.class)))
        }
    )
    @GetMapping
    public ResponseEntity<List<ShipperProjectResponseDTO>> getByUserId(@RequestParam String userId) {
    	log.info("Request getByUserId : {}",  userId);
        return ResponseEntity.ok(service.findByUserId(userId));
    }

    @Operation(
        summary = "Get a shipper project by projectId (_id)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Found",
                content = @Content(schema = @Schema(implementation = ShipperProjectResponseDTO.class)))
        }
    )
    @GetMapping("/{projectId}")
    public ResponseEntity<?> getByProjectId(@PathVariable String projectId) {
    	log.info("Request getByProjectId : {}",  projectId);
        var response = service.getByProjectId(projectId);
        if (response == null) {
            // Mirror Node behavior for "not found": return {}
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Create a new shipper project",
        description = "Validates userId and name, then creates a new project with a generated _id.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Created",
                content = @Content(schema = @Schema(implementation = ShipperProjectResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error")
        }
    )
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ShipperProjectRequestDTO requestDTO) {
    	log.info("Request create : {}",  requestDTO);
        var saved = service.create(requestDTO);
        // Mirror Node: return raw document content (not an envelope)
        return ResponseEntity.ok(saved);
    }

    @Operation(
        summary = "Update a shipper project by projectId/_id",
        description = "Validates id and name; returns 404 if not found, 400 if invalid.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Updated",
                content = @Content(schema = @Schema(implementation = ShipperProjectResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found")
        }
    )
    @PutMapping
    public ResponseEntity<?> update(@Valid @RequestBody ShipperProjectUpdateRequestDTO requestDTO) {
    	log.info("Request update : {}",  requestDTO);
        var updated = service.update(requestDTO);
        return ResponseEntity.ok(updated);
    }

    @Operation(
        summary = "Delete a project by projectId and return remaining projects for the same user",
        responses = {
            @ApiResponse(responseCode = "200", description = "Deleted and list returned"),
            @ApiResponse(responseCode = "404", description = "Project not found")
        }
    )
    @DeleteMapping
    public ResponseEntity<?> delete(@RequestParam String projectId) {
    	log.info("Request delete : {}",  projectId);
        var result = service.deleteByProjectId(projectId);
        return ResponseEntity.ok(Map.of(
                "message", "Projekt erfolgreich gelöscht",
                "projects", result
        ));
    }
}
