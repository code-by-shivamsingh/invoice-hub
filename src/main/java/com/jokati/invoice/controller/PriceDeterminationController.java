
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.PriceDeterminationResponseDTO;
import com.jokati.invoice.service.PriceDeterminationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/price-determination")
@Tag(name = "Price Determination", description = "Aggregate documents for price determination")
@RequiredArgsConstructor
public class PriceDeterminationController {

    private final PriceDeterminationService service;

    @Operation(summary = "Get aggregated documents for price determination by projectId")
    @GetMapping
    public ResponseEntity<?> get(@RequestParam(required = false) String projectId) {
        try {
            if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
                // Mirror Node semantics by returning undefined/empty -> {}
                return ResponseEntity.ok(Map.of());
            }

            var docsOpt = service.aggregateByProjectId(projectId);
            if (docsOpt.isEmpty()) {
                // Invalid ObjectId or not found -> {}
                return ResponseEntity.ok(Map.of());
            }

            return ResponseEntity.ok(docsOpt.get());
        } catch (Exception e) {
            // Node returns 500 with undefined
            return ResponseEntity.status(500).build();
        }
    }
}
