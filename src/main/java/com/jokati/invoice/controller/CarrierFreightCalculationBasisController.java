
package com.jokati.invoice.controller;

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
import com.jokati.invoice.dto.CarrierFreightCalculationBasisRequestDTO;
import com.jokati.invoice.model.CarrierFreightCalculationBasis;
import com.jokati.invoice.service.CarrierFreightCalculationBasisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
     * GET: mirrors Node GET — fetch by carrierProjectId and return ONLY Countries (or {}).
     * Node returns 200 with {} when not found or invalid.
     */
    @Operation(summary = "Get Countries by carrierProjectId")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> getCountries(@RequestParam(required = false) String carrierProjectId) {
        log.info("Request getCountries : {}", carrierProjectId);

        if (carrierProjectId == null || carrierProjectId.isBlank() || "null".equalsIgnoreCase(carrierProjectId)) {
            return ResponseUtil.okEmpty("OK");
        }

        return service.findByCarrierProjectId(carrierProjectId)
                .map(doc -> {
                    Map<String, Object> countries = doc.getCountries() == null ? Map.of() : doc.getCountries();
                    return ResponseUtil.okObject(countries, "Countries fetched successfully");
                })
                .orElse(ResponseUtil.okEmpty("OK"));
    }

    /**
     * POST: create with _id = carrierProjectId (like Mongoose new doc with given _id).
     * Returns saved document (full doc in data).
     */
    @Operation(summary = "Create carrier freight calculation basis")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> create(@RequestBody CarrierFreightCalculationBasisRequestDTO request) {
        log.info("Request create : {}", request);

        if (request.getCarrierProjectId() == null || request.getCarrierProjectId().isBlank()) {
            throw new IllegalArgumentException("carrierProjectId is required");
        }

        CarrierFreightCalculationBasis entity = CarrierFreightCalculationBasis.builder()
                .id(request.getCarrierProjectId())
                .countries(request.getCountries())
                .build();

        CarrierFreightCalculationBasis saved = service.create(entity);

        return ResponseUtil.okObject(saved, "Frachtberechnung gespeichert");
    }

    /**
     * PUT: upsert by carrierProjectId — matches findByIdAndUpdate(..., upsert:true, returnDocument:"after").
     * Returns updated document.
     */
    @Operation(summary = "Upsert carrier freight calculation basis")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> update(@RequestBody CarrierFreightCalculationBasisRequestDTO request) {
        log.info("Request update : {}", request);

        if (request.getCarrierProjectId() == null || request.getCarrierProjectId().isBlank()) {
            throw new IllegalArgumentException("carrierProjectId is required");
        }

        CarrierFreightCalculationBasis entity = CarrierFreightCalculationBasis.builder()
                .id(request.getCarrierProjectId())
                .countries(request.getCountries())
                .build();

        CarrierFreightCalculationBasis updated = service.upsert(request.getCarrierProjectId(), entity);

        return ResponseUtil.okObject(updated, "Frachtberechnung aktualisiert");
    }

    /**
     * DELETE: placeholder — Node handler is empty (we return 200 with {} + message).
     */
    @Operation(summary = "Delete carrier freight calculation basis by projectId (optional)")
    @DeleteMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String projectId) {
        log.info("Request delete : {}", projectId);
        service.deleteById(projectId);
        return ResponseUtil.okEmpty("Frachtberechnung gelöscht (falls vorhanden)");
    }
}
