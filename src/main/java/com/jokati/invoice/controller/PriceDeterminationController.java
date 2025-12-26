
package com.jokati.invoice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.PriceDeterminationResponseDTO;
import com.jokati.invoice.service.PriceDeterminationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/price-determination")
@Tag(name = "Price Determination", description = "Aggregate documents for price determination")
@RequiredArgsConstructor
public class PriceDeterminationController {

    private static final Logger log = LoggerFactory.getLogger(PriceDeterminationController.class);

    private final PriceDeterminationService service;

    @Operation(summary = "Get aggregated documents for price determination by projectId")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> get(@RequestParam(required = false) String projectId) {
        log.info("PriceDeterminationController.get : projectId={}", projectId);

        if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
            // Mirror Node semantics by returning {}
            return ResponseUtil.okEmpty("OK");
        }

        var docsOpt = service.aggregateByProjectId(projectId);
        if (docsOpt.isEmpty()) {
            // Invalid ObjectId or not found -> {}
            return ResponseUtil.okEmpty("OK");
        }

        PriceDeterminationResponseDTO data = docsOpt.get();
        return ResponseUtil.ok(data, "Price determination data fetched successfully");
    }
}
