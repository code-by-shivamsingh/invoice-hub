
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.CarrierFreightCalculationBasisRequestDTO;
import com.jokati.invoice.dto.CarrierFreightCalculationBasisResponseDTO;
import com.jokati.invoice.model.CarrierFreightCalculationBasis;
import com.jokati.invoice.service.CarrierFreightCalculationBasisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/carrier-freight-calculation-basis")
@Tag(name = "Carrier Freight Calculation Basis API", description = "Manage carrier freight calculation basis")
public class CarrierFreightCalculationBasisController {

	private static final Logger log = LoggerFactory.getLogger(CarrierFreightCalculationBasisController.class);
    private final CarrierFreightCalculationBasisService service;

    public CarrierFreightCalculationBasisController(CarrierFreightCalculationBasisService service) {
        this.service = service;
    }

    /**
     * GET: mirrors Node GET — fetch by projectId and return ONLY Countries (or {}).
     * Node code returns {} with 200 when not found.
     */
    @Operation(summary = "Get Countries by carrierProjectId")
    @GetMapping
    public ResponseEntity<?> getCountries(@RequestParam(required = false) String carrierProjectId) {
    	log.info("Request getCountries : {}",  carrierProjectId);
        if (carrierProjectId == null || carrierProjectId.isBlank() || "null".equals(carrierProjectId)) {
            return ResponseEntity.ok(Map.of());
        }

        return service.findByCarrierProjectId(carrierProjectId)
                .map(doc -> ResponseEntity.ok(doc.getCountries() == null ? Map.of() : doc.getCountries()))
                .orElse(ResponseEntity.ok(Map.of()));
    }
    
    // might be needed in future
    
//    @Operation(summary = "Get Countries by projectId")
//    @GetMapping("/by-project")
//    public ResponseEntity<?> getCountriesByProjectId(@RequestParam(required = false) String projectId) {
//    	log.info("Request getCountries : {}",  projectId);
//        if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
//            return ResponseEntity.ok(Map.of());
//        }
//
//        return service.findByProjectId(projectId)
//                .map(doc -> ResponseEntity.ok(doc.getCountries() == null ? Map.of() : doc.getCountries()))
//                .orElse(ResponseEntity.ok(Map.of()));
//    }

    /**
     * POST: create with _id = carrierProjectId (like Mongoose new doc with given _id).
     * Returns saved document (like Node spreading _doc).
     */
    @Operation(summary = "Create carrier freight calculation basis")
    @PostMapping
    public ResponseEntity<CarrierFreightCalculationBasisResponseDTO> create(
            @RequestBody CarrierFreightCalculationBasisRequestDTO request) {
    	
    	log.info("Request create : {}",  request);

        if (request.getCarrierProjectId() == null || request.getCarrierProjectId().isBlank()) {
            return ResponseEntity.badRequest().body(CarrierFreightCalculationBasisResponseDTO.builder()
                    .message("carrierProjectId is required")
                    .carrierFreightCalculationBasis(null)
                    .build());
        }

        CarrierFreightCalculationBasis entity = CarrierFreightCalculationBasis.builder()
                .id(request.getCarrierProjectId())
                .countries(request.getCountries())
                .build();

        CarrierFreightCalculationBasis saved = service.create(entity);

        return ResponseEntity.ok(CarrierFreightCalculationBasisResponseDTO.builder()
                .message("Frachtberechnung gespeichert")
                .carrierFreightCalculationBasis(saved)
                .build());
    }

    /**
     * PUT: upsert by carrierProjectId — matches Mongoose findByIdAndUpdate(..., { upsert: true, returnDocument: "after" })
     * Returns updated document.
     */
    @Operation(summary = "Upsert carrier freight calculation basis")
    @PutMapping
    public ResponseEntity<CarrierFreightCalculationBasisResponseDTO> update(
            @RequestBody CarrierFreightCalculationBasisRequestDTO request) {

    	log.info("Request update : {}",  request);
    	
        if (request.getCarrierProjectId() == null || request.getCarrierProjectId().isBlank()) {
            return ResponseEntity.badRequest().body(CarrierFreightCalculationBasisResponseDTO.builder()
                    .message("carrierProjectId is required")
                    .carrierFreightCalculationBasis(null)
                    .build());
        }

        CarrierFreightCalculationBasis entity = CarrierFreightCalculationBasis.builder()
                .id(request.getCarrierProjectId())
                .countries(request.getCountries())
                .build();

        CarrierFreightCalculationBasis updated = service.upsert(request.getCarrierProjectId(), entity);

        return ResponseEntity.ok(CarrierFreightCalculationBasisResponseDTO.builder()
                .message("Frachtberechnung aktualisiert")
                .carrierFreightCalculationBasis(updated)
                .build());
    }

    /**
     * DELETE: placeholder — Node handler is empty.
     */
    @Operation(summary = "Delete carrier freight calculation basis by projectId (optional)")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<CarrierFreightCalculationBasisResponseDTO> delete(@PathVariable String projectId) {
    	log.info("Request delete : {}",  projectId);
        service.deleteById(projectId);
        return ResponseEntity.ok(CarrierFreightCalculationBasisResponseDTO.builder()
                .message("Frachtberechnung gelöscht (falls vorhanden)")
                .carrierFreightCalculationBasis(null)
                .build());
    }
}
