package com.jokati.invoice.controller;

import com.jokati.invoice.dto.ShipperFreightCalculationBasisRequestDTO;
import com.jokati.invoice.dto.ShipperFreightCalculationBasisResponseDTO;
import com.jokati.invoice.model.ShipperFreightCalculationBasis;
import com.jokati.invoice.service.ShipperFreightCalculationBasisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipper-freight-calculation-basis")
@Tag(name = "Shipper Freight Calculation Basis API", description = "Manage freight calculation basis")
public class ShipperFreightCalculationBasisController {
	
	private final ShipperFreightCalculationBasisService service;
	
	public ShipperFreightCalculationBasisController(ShipperFreightCalculationBasisService service) {
		this.service =service;
	}
	
	@Operation(summary = "Create freight calculation basis")
    @PostMapping
    public ResponseEntity<ShipperFreightCalculationBasisResponseDTO> create(@RequestBody ShipperFreightCalculationBasisRequestDTO request) {
        ShipperFreightCalculationBasis entity = ShipperFreightCalculationBasis.builder()
                .projectId(request.getProjectId())
                .calculationBasis(request.getCalculationBasis())
                .build();

        ShipperFreightCalculationBasis saved = service.save(entity);

        return ResponseEntity.ok(ShipperFreightCalculationBasisResponseDTO.builder()
                .message("Freight calculation basis saved successfully")
                .shipperFreightCalculationBasis(saved)
                .build());
    }

    @Operation(summary = "Update freight calculation basis")
    @PutMapping("/{id}")
    public ResponseEntity<ShipperFreightCalculationBasisResponseDTO> update(@PathVariable String id,
                                                                            @RequestBody ShipperFreightCalculationBasisRequestDTO request) {
        ShipperFreightCalculationBasis entity = ShipperFreightCalculationBasis.builder()
                .projectId(request.getProjectId())
                .calculationBasis(request.getCalculationBasis())
                .build();

        ShipperFreightCalculationBasis updated = service.update(id, entity);

        return ResponseEntity.ok(ShipperFreightCalculationBasisResponseDTO.builder()
                .message("Freight calculation basis updated successfully")
                .shipperFreightCalculationBasis(updated)
                .build());
    }

    @Operation(summary = "Get freight calculation basis by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ShipperFreightCalculationBasisResponseDTO> get(@PathVariable String id) {
        return service.findById(id)
                .map(basis -> ResponseEntity.ok(ShipperFreightCalculationBasisResponseDTO.builder()
                        .message("Freight calculation basis fetched successfully")
                        .shipperFreightCalculationBasis(basis)
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete freight calculation basis by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ShipperFreightCalculationBasisResponseDTO> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ShipperFreightCalculationBasisResponseDTO.builder()
                .message("Freight calculation basis deleted successfully")
                .shipperFreightCalculationBasis(null)
                .build());
    }

}

