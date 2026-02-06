
package com.jokati.invoice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.jokati.invoice.dto.ShipperExtraCostsRequestDTO;
import com.jokati.invoice.dto.ShipperExtraCostsResponseDTO;
import com.jokati.invoice.model.ShipperExtraCosts;
import com.jokati.invoice.service.ShipperExtraCostsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shipper-extra-costs")
@Tag(name = "Shipper Extra Costs API", description = "Manage shipper extra costs")
public class ShipperExtraCostsController {
    private static final Logger log = LoggerFactory.getLogger(ShipperExtraCostsController.class);

    private final ShipperExtraCostsService service;

    public ShipperExtraCostsController(ShipperExtraCostsService service) {
        this.service = service;
    }

    @Operation(summary = "Create shipper extra costs")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(@Valid @RequestBody ShipperExtraCostsRequestDTO request) {
        log.info("Request create : {}", request);

        ShipperExtraCosts entity = toEntity(request);
        ShipperExtraCosts saved = service.save(entity);

        return ResponseUtil.okObject(
                toResponseDTO(saved),
                "Extra costs saved successfully"
        );
    }

    @Operation(summary = "Update shipper extra costs")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable String id,
                                                      @Valid @RequestBody ShipperExtraCostsRequestDTO request) {
        log.info("Request update : id={}, request={}", id, request);

        ShipperExtraCosts entity = toEntity(request);
        ShipperExtraCosts updated = service.update(id, entity);

        return ResponseUtil.okObject(
                toResponseDTO(updated),
                "Extra costs updated successfully"
        );
    }

    @Operation(summary = "Get shipper extra costs by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> get(@PathVariable String id) {
        log.info("Request get : {}", id);

        return service.findById(id)
                .map(costs -> ResponseUtil.okObject(
                        toResponseDTO(costs),
                        "Extra costs fetched successfully"
                ))
                // Node-style: 200 OK with {} if invalid/missing
                .orElse(ResponseUtil.okEmpty("OK"));
    }
    
    @Operation(summary = "Get shipper extra costs by projectID")
    @GetMapping("/projectid")
    public ResponseEntity<ApiResponse<Object>> getShipperExtraCostByProjectId(@RequestParam String projectId) {
        log.info("Request get Project Id : {}", projectId);

        return service.findByProjectId(projectId)
                .map(costs -> ResponseUtil.okObject(
                        toResponseDTO(costs),
                        "Extra costs fetched successfully"
                ))
                // Node-style: 200 OK with {} if invalid/missing
                .orElse(ResponseUtil.okEmpty("OK"));
    }

    @Operation(summary = "Delete shipper extra costs by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String id) {
        log.info("Request delete : {}", id);
        service.delete(id);
        // Return node-style {} with message in envelope
        return ResponseUtil.okEmpty("Extra costs deleted successfully");
    }
    
    @Operation(summary = "Get country list by projectId")
    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<Object>> getCountries(@RequestParam String projectId) {
        log.info("Request get countries projectId={}", projectId);

        var countries = service.getCountriesByProjectId(projectId);

        return ResponseUtil.okObject(countries, "Countries fetched successfully");
    }

    @Operation(summary = "Get shipper extra cost by projectId and countryCode")
    @GetMapping("/country-cost")
    public ResponseEntity<ApiResponse<Object>> getCountryCost(
            @RequestParam String projectId,
            @RequestParam(required = false) String countryCode) {

        log.info("Request get extra cost projectId={}, countryCode={}", projectId, countryCode);

        var cost = service.getExtraCostByCountry(projectId, countryCode);
        
        if (cost == null) {
	        return ResponseUtil.okEmpty("No Extra cost found");
	    }

        return ResponseUtil.okObject(cost, "Extra cost fetched successfully");
    }




    /* ---------- mapping helpers ---------- */

    private ShipperExtraCosts toEntity(ShipperExtraCostsRequestDTO request) {
        return ShipperExtraCosts.builder()
                .projectId(request.getProjectId())
                .extraCosts(request.getExtraCosts())
                .build();
    }

    private ShipperExtraCostsResponseDTO toResponseDTO(ShipperExtraCosts entity) {
        return ShipperExtraCostsResponseDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .extraCosts(entity.getExtraCosts())
                .build();
    }
}
