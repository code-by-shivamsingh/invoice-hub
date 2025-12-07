
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.ShipperFreightCalculationBasisRequestDTO;
import com.jokati.invoice.dto.ShipperFreightCalculationBasisResponseDTO;
import com.jokati.invoice.model.ShipperFreightCalculationBasis;
import com.jokati.invoice.service.ShipperFreightCalculationBasisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipper-freight-calculation-basis")
@Tag(name = "Shipper Freight Calculation Basis API", description = "Manage freight calculation basis")
public class ShipperFreightCalculationBasisController {
	private static final Logger log = LoggerFactory.getLogger(ShipperFreightCalculationBasisController.class);

    private final ShipperFreightCalculationBasisService service;

    public ShipperFreightCalculationBasisController(ShipperFreightCalculationBasisService service) {
        this.service = service;
    }

    @Operation(summary = "Create freight calculation basis")
    @PostMapping
    public ResponseEntity<ShipperFreightCalculationBasisResponseDTO> create(
            @Valid @RequestBody ShipperFreightCalculationBasisRequestDTO request) {
    	log.info("Request create : {}",  request);

        ShipperFreightCalculationBasis entity = service.fromRequestDTO(request);
        ShipperFreightCalculationBasis saved = service.save(entity);

        return ResponseEntity.ok(
                toResponseDTO(saved, "Freight calculation basis saved successfully")
        );
    }

    @Operation(summary = "Update freight calculation basis")
    @PutMapping("/{id}")
    public ResponseEntity<ShipperFreightCalculationBasisResponseDTO> update(
            @PathVariable String id,
            @Valid @RequestBody ShipperFreightCalculationBasisRequestDTO request) {
    	log.info("Request update bu id  : {}, and request {}", id, request);

        ShipperFreightCalculationBasis entity = service.fromRequestDTO(request);
        ShipperFreightCalculationBasis updated = service.update(id, entity);

        return ResponseEntity.ok(
                toResponseDTO(updated, "Freight calculation basis updated successfully")
        );
    }

    @Operation(summary = "Get freight calculation basis by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ShipperFreightCalculationBasisResponseDTO> get(@PathVariable String id) {
    	log.info("Request get : {}",  id);
        return service.findById(id)
                .map(basis -> ResponseEntity.ok(
                        toResponseDTO(basis, "Freight calculation basis fetched successfully")
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete freight calculation basis by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ShipperFreightCalculationBasisResponseDTO> delete(@PathVariable String id) {
    	log.info("Request delete : {}",  id);
        service.delete(id);
        return ResponseEntity.ok(
                ShipperFreightCalculationBasisResponseDTO.builder()
                        .message("Freight calculation basis deleted successfully")
                        .id(null)
                        .projectId(null)
                        .carrierProjectId(null)
                        .countries(null)
                        .firebaseId(null)
                        .extra(null)
                        .createdAt(null)
                        .updatedAt(null)
                        .build()
        );
    }

    /* ---------- mapping helper ---------- */

    private ShipperFreightCalculationBasisResponseDTO toResponseDTO(ShipperFreightCalculationBasis entity, String message) {
        return ShipperFreightCalculationBasisResponseDTO.builder()
                .message(message)
                .id(entity.getId() != null ? entity.getId().toHexString() : null)
                .projectId(entity.getProjectId())
                .carrierProjectId(entity.getCarrierProjectId())
                .countries(entity.getCountries())
                .firebaseId(entity.getFirebaseId())
                .extra(entity.getExtra())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
