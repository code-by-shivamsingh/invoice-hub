
package com.jokati.invoice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
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

    private static final Logger log = LoggerFactory.getLogger(ToleranceLimitsController.class);

    private final ToleranceLimitsService service;

    @Operation(summary = "Create tolerance limits for a user (one record per companyId)")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(@Valid @RequestBody ToleranceLimitsRequestDTO request) {
        log.info("Tolerance create: companyId={}", request.getCompanyId());
        // Input validation via @Valid; additional guard if needed:
        if (request.getCompanyId() == null || request.getCompanyId().isBlank()) {
            throw new IllegalArgumentException("companyId must not be blank");
        }
        ToleranceLimitsResponseDTO dto = service.create(request);
        return ResponseUtil.okObject(dto, "Tolerance limits created successfully");
    }

    @Operation(summary = "Get tolerance limits by companyId")
    @GetMapping("/{companyId}")
    public ResponseEntity<ApiResponse<Object>> get(@PathVariable String companyId) {
        log.info("Tolerance get: companyId={}", companyId);
        ToleranceLimitsResponseDTO dto = service.getByCompanyId(companyId);
        return ResponseUtil.okObject(dto, "Tolerance limits fetched successfully");
    }

//    @Operation(summary = "Replace tolerance limits (PUT) by companyId")
//    @PutMapping("/{companyId}")
//    public ResponseEntity<ApiResponse<Object>> replace(
//            @PathVariable String companyId,
//            @Valid @RequestBody ToleranceLimitsRequestDTO request) {
//        log.info("Tolerance replace: companyId={}", companyId);
//        ToleranceLimitsResponseDTO dto = service.replace(companyId, request);
//        return ResponseUtil.okObject(dto, "Tolerance limits replaced successfully");
//    }

    @Operation(summary = "Patch tolerance limits by companyId")
    @PatchMapping("/{companyId}")
    public ResponseEntity<ApiResponse<Object>> patch(
            @PathVariable String companyId,
            @Valid @RequestBody ToleranceLimitsPatchRequestDTO patch) {
        log.info("Tolerance patch: companyId={}", companyId);
        ToleranceLimitsResponseDTO dto = service.patch(companyId, patch);
        return ResponseUtil.okObject(dto, "Tolerance limits patched successfully");
    }

    @Operation(summary = "Delete tolerance limits by companyId")
    @DeleteMapping("/{companyId}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String companyId) {
        log.info("Tolerance delete: companyId={}", companyId);
        service.deleteByCompanyId(companyId);
        // Consistent with your other controllers: 200 with {} and message in envelope
        return ResponseUtil.okEmpty("Tolerance limits deleted successfully");
    }

    @Operation(summary = "Remove ancillary cost tolerance by designation for a user")
    @DeleteMapping("/ancillary-tolerances/{companyId}/{designation}")
    public ResponseEntity<ApiResponse<Object>> removeAncillary(
            @PathVariable String companyId,
            @PathVariable String designation) {
        log.info("Tolerance removeAncillary: companyId={}, designation={}", companyId, designation);
        ToleranceLimitsResponseDTO dto = service.removeAncillary(companyId, designation);
        return ResponseUtil.okObject(dto, "Ancillary tolerance removed successfully");
    }
}
