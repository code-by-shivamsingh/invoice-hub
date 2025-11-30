package com.jokati.invoice.controller;

import com.jokati.invoice.dto.ShipperExtraCostsRequestDTO;
import com.jokati.invoice.dto.ShipperExtraCostsResponseDTO;
import com.jokati.invoice.model.ShipperExtraCosts;
import com.jokati.invoice.service.ShipperExtraCostsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipper-extra-costs")
@Tag(name = "Shipper Extra Costs API", description = "Manage shipper extra costs")
public class ShipperExtraCostsController {
	private final ShipperExtraCostsService service;
	public ShipperExtraCostsController (ShipperExtraCostsService service) {
        this.service = service;
    }


	@Operation(summary = "Create shipper extra costs")
	@PostMapping
	public ResponseEntity<ShipperExtraCostsResponseDTO> create(@RequestBody ShipperExtraCostsRequestDTO request) {
	    // Build entity
	    ShipperExtraCosts entity = ShipperExtraCosts.builder()
	            .projectId(request.getProjectId())
	            .extraCosts(request.getExtraCosts())
	            .build();
	
	    // Save entity
	    ShipperExtraCosts saved = service.save(entity);
	
	    // Return response
	    return ResponseEntity.ok(ShipperExtraCostsResponseDTO.builder()
	            .message("Extra costs saved successfully")
	            .extraCosts(saved)
	            .build());
	}


    @Operation(summary = "Update shipper extra costs")
    @PutMapping("/{id}")
    public ResponseEntity<ShipperExtraCostsResponseDTO> update(@PathVariable String id,
                                                               @RequestBody ShipperExtraCostsRequestDTO request) {
        ShipperExtraCosts entity = ShipperExtraCosts.builder()
                .projectId(request.getProjectId())
                .extraCosts(request.getExtraCosts())
                .build();

        ShipperExtraCosts updated = service.update(id, entity);

        return ResponseEntity.ok(ShipperExtraCostsResponseDTO.builder()
                .message("Extra costs updated successfully")
                .extraCosts(updated)
                .build());
    }

    @Operation(summary = "Get shipper extra costs by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ShipperExtraCostsResponseDTO> get(@PathVariable String id) {
        return service.findById(id)
                .map(costs -> ResponseEntity.ok(ShipperExtraCostsResponseDTO.builder()
                        .message("Extra costs fetched successfully")
                        .extraCosts(costs)
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete shipper extra costs by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ShipperExtraCostsResponseDTO> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ShipperExtraCostsResponseDTO.builder()
                .message("Extra costs deleted successfully")
                .extraCosts(null)
                .build());
    }
}
