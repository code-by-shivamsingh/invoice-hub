
package com.jokati.invoice.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.ShipperProjectRequestDTO;
import com.jokati.invoice.dto.ShipperProjectResponseDTO;
import com.jokati.invoice.dto.ShipperProjectUpdateRequestDTO;
import com.jokati.invoice.service.ShipperProjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Shipper Projects", description = "APIs to manage shipper projects")
@RestController
@RequestMapping("/api/v1/shipper-projects")
@RequiredArgsConstructor
public class ShipperProjectsController {
    private static final Logger log = LoggerFactory.getLogger(ShipperProjectsController.class);

    private final ShipperProjectService service;

    @Operation(summary = "Get all projects for a user")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShipperProjectResponseDTO>>> getByUserId(@RequestParam String userId) {
        log.info("Request getByUserId : {}", userId);
        var data = service.findByUserId(userId);
        return ResponseUtil.ok(data, "Projects fetched successfully");
    }

    @Operation(
        summary = "Get a shipper project by projectId (_id)",
        description = "Returns 200 with {} (empty object) if not found or invalid id to mirror Node behavior."
    )
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Object>> getByProjectId(@PathVariable String projectId) {
        log.info("Request getByProjectId : {}", projectId);
        var response = service.getByProjectId(projectId);
        if (response == null) {
            // Node-style: 200 OK with {}
            return ResponseUtil.okEmpty("OK");
        }
        return ResponseUtil.ok(response, "Project fetched successfully");
    }

    @Operation(
        summary = "Create a new shipper project",
        description = "Validates userId and name, then creates a new project with a generated _id."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ShipperProjectResponseDTO>> create(
            @Valid @RequestBody ShipperProjectRequestDTO requestDTO) {
        log.info("Request create : {}", requestDTO);
        
        var saved = service.create(requestDTO);
        // Using 200 OK to mirror your Node behavior; if you prefer 201, use ResponseUtil.created(...)
        return ResponseUtil.ok(saved, "Project created successfully");
    }

//    @Operation(
//        summary = "Update a shipper project by projectId/_id",
//        description = "Validates id and name; returns 404 if not found, 400 if invalid."
//    )
//    @PutMapping
//    public ResponseEntity<ApiResponse<ShipperProjectResponseDTO>> update(
//            @Valid @RequestBody ShipperProjectUpdateRequestDTO requestDTO) {
//        log.info("Request update : {}", requestDTO);
//        var updated = service.update(requestDTO);
//        return ResponseUtil.ok(updated, "Project updated successfully");
//    }

    @Operation(
        summary = "Delete a project by projectId and return remaining projects for the same user"
    )
    @DeleteMapping
    public ResponseEntity<ApiResponse<List<ShipperProjectResponseDTO>>> delete(@RequestParam String projectId) {
        log.info("Request delete : {}", projectId);
        var remaining = service.deleteByProjectId(projectId);
        // Message sits in the envelope; data contains only remaining projects
        return ResponseUtil.ok(null,"Project deleted successfully");
    }
}
