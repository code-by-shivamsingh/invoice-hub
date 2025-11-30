
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.DieselFloaterResponseDTO;
import com.jokati.invoice.model.DieselFloater;
import com.jokati.invoice.service.DieselFloaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diesel-floater")
@Tag(name = "Diesel Floater API", description = "Manage Diesel Floater data")
public class DieselFloaterController {

    private final DieselFloaterService service;

    public DieselFloaterController(DieselFloaterService service) {
        this.service = service;
    }

    @Operation(summary = "Create or update Diesel Floater data")
    @PutMapping
    public ResponseEntity<DieselFloaterResponseDTO> createOrUpdate(@RequestBody Map<String, Object> dieselFloaterJson) {
        try {
            List<DieselFloater> existingData = service.findAll();
            DieselFloater dieselFloater;

            if (existingData.isEmpty()) {
                dieselFloater = DieselFloater.builder()
                        .years(dieselFloaterJson)
                        .build();
                service.save(dieselFloater);
                return ResponseEntity.status(201).body(DieselFloaterResponseDTO.builder()
                        .message("DieselFloater created")
                        .years(dieselFloaterJson)
                        .build());
            } else {
                String id = existingData.get(0).getId();
                dieselFloater = DieselFloater.builder()
                        .id(id)
                        .years(dieselFloaterJson)
                        .build();
                service.update(id, dieselFloater);
                return ResponseEntity.ok(DieselFloaterResponseDTO.builder()
                        .message("DieselFloater updated")
                        .years(dieselFloaterJson)
                        .build());
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(DieselFloaterResponseDTO.builder()
                    .message("Server error: " + e.getMessage())
                    .years(null)
                    .build());
        }
    }

    @Operation(summary = "Get Diesel Floater matrix")
    @GetMapping
    public ResponseEntity<DieselFloaterResponseDTO> getMatrix() {
        List<DieselFloater> data = service.findAll();
        if (data.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(DieselFloaterResponseDTO.builder()
                .message("DieselFloater matrix fetched")
                .years(data.get(0).getYears())
                .build());
    }

    @Operation(summary = "Get Diesel Floater sources")
    @GetMapping("/sources")
    public ResponseEntity<DieselFloaterResponseDTO> getSources() {
        List<DieselFloater> data = service.findAll();
        if (data.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        Map<String, Object> years = data.get(0).getYears();
        String firstYear = years.keySet().iterator().next();
        List<Map<String, Object>> firstYearData = (List<Map<String, Object>>) years.get(firstYear);
        List<String> sources = firstYearData.get(0).keySet().stream().filter(key -> !key.equals("_id")).toList();

        return ResponseEntity.ok(DieselFloaterResponseDTO.builder()
                .message("DieselFloater sources fetched")
                .years(sources)
                .build());
    }
}
