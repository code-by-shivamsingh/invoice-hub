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

    @Operation(summary = "Create tolerance limits for a user (one record per companyId)")
    @PostMapping
    public ResponseEntity<ToleranceLimitsResponseDTO> create(
            @Valid @RequestBody ToleranceLimitsRequestDTO request) throws Exception {
        if (request.getCompanyId() == null) {
            throw new Exception("companyId in must");
        }
        return ResponseEntity.ok(service.create(request));
    }

    @Operation(summary = "Get tolerance limits by companyId")
    @GetMapping("/{companyId}")
    public ResponseEntity<ToleranceLimitsResponseDTO> get(@PathVariable String companyId) throws Exception {
        return ResponseEntity.ok(service.getByCompanyId(companyId));
    }

    @Operation(summary = "Replace tolerance limits (PUT) by companyId")
    @PutMapping("/{companyId}")
    public ResponseEntity<ToleranceLimitsResponseDTO> replace(
            @PathVariable String companyId,
            @Valid @RequestBody ToleranceLimitsRequestDTO request) throws Exception {
        return ResponseEntity.ok(service.replace(companyId, request));
    }

    @Operation(summary = "Patch tolerance limits by companyId")
    @PatchMapping("/{companyId}")
    public ResponseEntity<ToleranceLimitsResponseDTO> patch(
            @PathVariable String companyId,
            @Valid @RequestBody ToleranceLimitsPatchRequestDTO patch) throws Exception {
        return ResponseEntity.ok(service.patch(companyId, patch));
    }

    @Operation(summary = "Delete tolerance limits by companyId")
    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> delete(@PathVariable String companyId) throws Exception {
        service.deleteByCompanyId(companyId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove ancillary cost tolerance by designation for a user")
    @DeleteMapping("/ancillary-tolerances/{companyId}/{designation}")
    public ResponseEntity<ToleranceLimitsResponseDTO> removeAncillary(
            @PathVariable String companyId,
            @PathVariable String designation) throws Exception {
        return ResponseEntity.ok(service.removeAncillary(companyId, designation));
    }
}
