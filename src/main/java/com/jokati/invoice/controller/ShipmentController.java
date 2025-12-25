
package com.jokati.invoice.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.dto.ShipmentRequestDTO;
import com.jokati.invoice.dto.ShipmentSaveResponseDTO;
import com.jokati.invoice.service.ShipmentService;
import com.jokati.invoice.service.ShipmentSummaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/shipment")
@Tag(name = "Shipment API", description = "Operations related to shipment data")
public class ShipmentController {

    private static final Logger log = LoggerFactory.getLogger(ShipmentController.class);
    private final ShipmentService service;

    public ShipmentController(ShipmentService service,ShipmentSummaryService serviceSummary) {
        this.service = service;
    }

    @Operation(
        summary = "Get shipment data by projectId",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Success",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ShipmentSaveResponseDTO.class)
                )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid projectId"),
            @ApiResponse(responseCode = "404", description = "Project not found")
        }
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ShipmentSaveResponseDTO> getShipmentData(@RequestParam @NotBlank String projectId) {
        log.info("GET /api/shipment projectId={}", projectId);
        Optional<ShipmentSaveResponseDTO> respOpt = service.getByProjectId(projectId);
        return respOpt.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(
        summary = "Save shipment data batch",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ShipmentRequestDTO.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Saved successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ShipmentSaveResponseDTO.class)
                )
            ),
            @ApiResponse(responseCode = "400", description = "Validation error")
        }
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ShipmentSaveResponseDTO> saveShipmentData(
            @Valid @org.springframework.web.bind.annotation.RequestBody ShipmentRequestDTO request) {

        log.info("POST /api/shipment projectId={}, carrierProjectId={}, items={}, append={}",
                request.getProjectId(),
                request.getCarrierProjectId(),
                request.getShipmentData() != null ? request.getShipmentData().size() : 0,
                request.isAppend());

        if (request.getProjectId() == null || request.getProjectId().isBlank()
                || request.getShipmentData() == null || request.getShipmentData().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ShipmentSaveResponseDTO response = service.saveBatch(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Delete shipment data by projectId",
        responses = {
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Project not found")
        }
    )
    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteShipmentData(@RequestParam @NotBlank String projectId) {
        log.info("DELETE /api/shipment projectId={}", projectId);
        boolean existed = service.deleteByProjectId(projectId);
        return existed ? ResponseEntity.noContent().build()
                       : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // ---------- Controller-level Exception Handling ----------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Validation failed");
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Constraint violation");
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Malformed JSON request");
        body.put("error", ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled error", ex);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
    
}

