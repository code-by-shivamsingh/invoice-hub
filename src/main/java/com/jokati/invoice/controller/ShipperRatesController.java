
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.ShipperRatesRequestDTO;
import com.jokati.invoice.dto.ShipperRatesResponseDTO;
import com.jokati.invoice.model.ShipperRates;
import com.jokati.invoice.service.ShipperRatesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipper-rates")
@Tag(name = "Shipper Rates API", description = "Manage shipper rates")
public class ShipperRatesController {
	
	private final ShipperRatesService service;

    public ShipperRatesController(ShipperRatesService service) {
        this.service = service;
    }

    @Operation(summary = "Create shipper rates")
    @PostMapping
    public ResponseEntity<ShipperRatesResponseDTO> create(@RequestBody ShipperRatesRequestDTO request) {
        ShipperRates entity = ShipperRates.builder()
                .projectId(request.getProjectId())
                .rates(request.getRates())
                .build();

        ShipperRates saved = service.save(entity);

        return ResponseEntity.ok(ShipperRatesResponseDTO.builder()
                .message("Shipper rates saved successfully")
                .shipperRates(saved)
                .build());
    }

    @Operation(summary = "Update shipper rates")
    @PutMapping("/{id}")
    public ResponseEntity<ShipperRatesResponseDTO> update(@PathVariable String id,
                                                          @RequestBody ShipperRatesRequestDTO request) {
        ShipperRates entity = ShipperRates.builder()
                .projectId(request.getProjectId())
                .rates(request.getRates())
                .build();

        ShipperRates updated = service.update(id, entity);

        return ResponseEntity.ok(ShipperRatesResponseDTO.builder()
                .message("Shipper rates updated successfully")
                .shipperRates(updated)
                .build());
    }

    @Operation(summary = "Get shipper rates by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ShipperRatesResponseDTO> get(@PathVariable String id) {
        return service.findById(id)
                .map(rates -> ResponseEntity.ok(ShipperRatesResponseDTO.builder()
                        .message("Shipper rates fetched successfully")
                        .shipperRates(rates)
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete shipper rates by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ShipperRatesResponseDTO> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ShipperRatesResponseDTO.builder()
                .message("Shipper rates deleted successfully")
                .shipperRates(null)
                .build());
    }
}
