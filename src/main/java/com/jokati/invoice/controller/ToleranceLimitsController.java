package com.jokati.invoice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.dto.ToleranceDTO;
import com.jokati.invoice.dto.ToleranceLimitsPatchRequestDTO;
import com.jokati.invoice.dto.ToleranceLimitsRequestDTO;
import com.jokati.invoice.dto.ToleranceLimitsResponseDTO;
import com.jokati.invoice.service.ToleranceLimitsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Tolerance Limits")
@RestController
@RequestMapping("/api/v1/tolerance-limits")
@RequiredArgsConstructor
public class ToleranceLimitsController {

    private final ToleranceLimitsService service;

    @Operation(summary = "Create tolerance limits for a user (one record per userId)")
    @PostMapping
    public ResponseEntity<ToleranceLimitsResponseDTO> create(
            @Valid @RequestBody ToleranceLimitsRequestDTO request) throws Exception {
        if (request.getUserId() == null) {
            throw new Exception("userId in must");
        }
        return ResponseEntity.ok(service.create(request));
    }

    @Operation(summary = "Get tolerance limits by userId")
    @GetMapping("/{userId}")
    public ResponseEntity<ToleranceLimitsResponseDTO> get(@PathVariable String userId) throws Exception {
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @Operation(summary = "Replace tolerance limits (PUT) by userId")
    @PutMapping("/{userId}")
    public ResponseEntity<ToleranceLimitsResponseDTO> replace(
            @PathVariable String userId,
            @Valid @RequestBody ToleranceLimitsRequestDTO request) throws Exception {
        return ResponseEntity.ok(service.replace(userId, request));
    }

    @Operation(summary = "Patch tolerance limits by userId")
    @PatchMapping("/{userId}")
    public ResponseEntity<ToleranceLimitsResponseDTO> patch(
            @PathVariable String userId,
            @Valid @RequestBody ToleranceLimitsPatchRequestDTO patch) throws Exception {
        return ResponseEntity.ok(service.patch(userId, patch));
    }

    @Operation(summary = "Delete tolerance limits by userId")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable String userId) throws Exception {
        service.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove ancillary cost tolerance by designation for a user")
    @DeleteMapping("/ancillary-tolerances/{userId}/{designation}")
    public ResponseEntity<ToleranceLimitsResponseDTO> removeAncillary(
            @PathVariable String userId,
            @PathVariable String designation) throws Exception {
        return ResponseEntity.ok(service.removeAncillary(userId, designation));
    }
}
