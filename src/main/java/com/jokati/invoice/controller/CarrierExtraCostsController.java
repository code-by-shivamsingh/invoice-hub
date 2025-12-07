
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.CarrierExtraCostsRequestDTO;
import com.jokati.invoice.dto.CarrierExtraCostsResponseDTO;
import com.jokati.invoice.model.CarrierExtraCosts;
import com.jokati.invoice.model.DieselFloater;
import com.jokati.invoice.repository.CarrierExtraCostsRepository;
import com.jokati.invoice.repository.DieselFloaterRepository;
import com.jokati.invoice.service.CarrierExtraCostsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carrier-extra-costs")
@Tag(name = "Carrier Extra Costs API", description = "Manage carrier extra costs")
public class CarrierExtraCostsController {
	private static final Logger log = LoggerFactory.getLogger(CarrierExtraCostsController.class);

private final CarrierExtraCostsService service;
    private final CarrierExtraCostsRepository carrierRepo;
    private final DieselFloaterRepository dieselRepo;

    public CarrierExtraCostsController(CarrierExtraCostsService service,
                                       CarrierExtraCostsRepository carrierRepo,
                                       DieselFloaterRepository dieselRepo) {
        this.service = service;
        this.carrierRepo = carrierRepo;
        this.dieselRepo = dieselRepo;
    }


    @Operation(summary = "Create carrier extra costs")
    @PostMapping
    public ResponseEntity<CarrierExtraCostsResponseDTO> create(@RequestBody CarrierExtraCostsRequestDTO request) {
    	log.info("Request create : {}",  request);
        CarrierExtraCosts entity = CarrierExtraCosts.builder()
                .carrierProjectId(request.getCarrierProjectId())
                .extraCosts(request.getExtraCosts())
                .build();
        CarrierExtraCosts saved = service.save(entity);
        return ResponseEntity.ok(CarrierExtraCostsResponseDTO.builder()
                .message("Carrier extra costs saved successfully")
                .carrierExtraCosts(saved)
                .build());
    }

    @Operation(summary = "Update carrier extra costs")
    @PutMapping("/{id}")
    public ResponseEntity<CarrierExtraCostsResponseDTO> update(@PathVariable String id,
                                                               @RequestBody CarrierExtraCostsRequestDTO request) {
    	log.info("Request update : {}",  request);
        CarrierExtraCosts entity = CarrierExtraCosts.builder()
                .carrierProjectId(request.getCarrierProjectId())
                .extraCosts(request.getExtraCosts())
                .build();
        CarrierExtraCosts updated = service.update(id, entity);
        return ResponseEntity.ok(CarrierExtraCostsResponseDTO.builder()
                .message("Carrier extra costs updated successfully")
                .carrierExtraCosts(updated)
                .build());
    }

    @Operation(summary = "Get carrier extra costs by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CarrierExtraCostsResponseDTO> get(@PathVariable String id) {
    	log.info("Request get : {}",  id);
        return service.findById(id)
                .map(costs -> ResponseEntity.ok(CarrierExtraCostsResponseDTO.builder()
                        .message("Carrier extra costs fetched successfully")
                        .carrierExtraCosts(costs)
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete carrier extra costs by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<CarrierExtraCostsResponseDTO> delete(@PathVariable String id) {
    	log.info("Request delete : {}",  id);
        service.delete(id);
        return ResponseEntity.ok(CarrierExtraCostsResponseDTO.builder()
                .message("Carrier extra costs deleted successfully")
                .carrierExtraCosts(null)
                .build());
    }

    @Operation(summary = "Fetch Carrier Extra Costs and Diesel Floater data")
    @GetMapping
    public ResponseEntity<CarrierExtraCostsResponseDTO> getCarrierExtraCosts(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false, defaultValue = "false") boolean findAll) {
    	
    	log.info("Request getCarrierExtraCosts : {} and find all : {}",  projectId,findAll);

        try {
            List<CarrierExtraCosts> extraCostsData = findAll
                    ? carrierRepo.findAll()
                    : (projectId != null ? List.of(carrierRepo.findById(projectId).orElse(null)) : List.of());

            List<DieselFloater> dieselFloaterData = dieselRepo.findAll();

            if (extraCostsData.isEmpty() || extraCostsData.get(0) == null) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(CarrierExtraCostsResponseDTO.builder()
                    .message("Data fetched successfully")
                    .carrierExtraCosts(extraCostsData)
                    .dieselFloater(dieselFloaterData.isEmpty() ? null : dieselFloaterData.get(0).getYears())
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(CarrierExtraCostsResponseDTO.builder()
                    .message("Server error: " + e.getMessage())
                    .carrierExtraCosts(null)
                    .dieselFloater(null)
                    .build());
        }
    }
}
