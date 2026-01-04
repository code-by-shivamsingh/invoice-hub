
package com.jokati.invoice.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.CarrierExtraCostsRequestDTO;
import com.jokati.invoice.dto.CarrierExtraCostsResponseDTO;
import com.jokati.invoice.model.CarrierExtraCosts;
import com.jokati.invoice.model.DieselFloater;
import com.jokati.invoice.repository.CarrierExtraCostsRepository;
import com.jokati.invoice.repository.DieselFloaterRepository;
import com.jokati.invoice.service.CarrierExtraCostsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/carrier-extra-costs")
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
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> create(@RequestBody CarrierExtraCostsRequestDTO request) {
        log.info("Request create : {}", request);

        CarrierExtraCosts entity = CarrierExtraCosts.builder()
                .carrierProjectId(request.getCarrierProjectId())
                .extraCosts(request.getExtraCosts())
                .build();

        CarrierExtraCosts saved = service.save(entity);

        // Return DTO in envelope; message in envelope
        CarrierExtraCostsResponseDTO dto = toResponseDTO(saved);
        return ResponseUtil.okObject(dto, "Carrier extra costs saved successfully");
    }

    @Operation(summary = "Update carrier extra costs")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable String id,
                                                      @RequestBody CarrierExtraCostsRequestDTO request) {
        log.info("Request update : {}", request);

        CarrierExtraCosts entity = CarrierExtraCosts.builder()
                .carrierProjectId(request.getCarrierProjectId())
                .extraCosts(request.getExtraCosts())
                .build();

        CarrierExtraCosts updated = service.update(id, entity);

        CarrierExtraCostsResponseDTO dto = toResponseDTO(updated);
        return ResponseUtil.okObject(dto, "Carrier extra costs updated successfully");
    }

    @Operation(summary = "Get carrier extra costs by ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> get(@PathVariable String id) {
        log.info("Request get : {}", id);

        // Node-style: 200 OK with {} when missing/invalid
        return service.findById(id)
                .map(costs -> ResponseUtil.okObject(toResponseDTO(costs), "Carrier extra costs fetched successfully"))
                .orElse(ResponseUtil.okEmpty("OK"));
    }

    @Operation(summary = "Delete carrier extra costs by ID")
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String id) {
        log.info("Request delete : {}", id);
        service.delete(id);
        return ResponseUtil.okEmpty("Carrier extra costs deleted successfully");
    }

    @Operation(summary = "Fetch Carrier Extra Costs and Diesel Floater data")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> getCarrierExtraCosts(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false, defaultValue = "false") boolean findAll) {

        log.info("Request getCarrierExtraCosts : projectId={}, findAll={}", projectId, findAll);

        // Build payload map for aggregated response
        List<CarrierExtraCosts> extraCostsData = findAll
                ? carrierRepo.findAll()
                : (projectId != null ? List.of(carrierRepo.findById(projectId).orElse(null)) : List.of());

        List<DieselFloater> dieselFloaterData = dieselRepo.findAll();

        if (extraCostsData.isEmpty() || extraCostsData.get(0) == null) {
            // Node-style: 200 OK with {} when not found
            return ResponseUtil.okEmpty("OK");
        }

        Map<String, Object> payload = Map.of(
                "carrierExtraCosts", extraCostsData,
                "dieselFloater", dieselFloaterData.isEmpty() ? null : dieselFloaterData.get(0).getYears()
        );

        return ResponseUtil.okObject(payload, "Data fetched successfully");
    }


/* ------------ mapping ------------ */
private CarrierExtraCostsResponseDTO toResponseDTO(CarrierExtraCosts entity) {
    return CarrierExtraCostsResponseDTO.builder()
            // or set via envelope and remove here
            .carrierExtraCosts(entity)                           // or entity.getExtraCosts() if you only want the map
            .dieselFloater(null)                                 // fill when available
            .build();
}

}
