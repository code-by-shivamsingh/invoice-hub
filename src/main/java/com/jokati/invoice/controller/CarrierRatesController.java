
package com.jokati.invoice.controller;

import java.util.Map;

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
import com.jokati.invoice.dto.CarrierRatesRequestDTO;
import com.jokati.invoice.model.CarrierRates;
import com.jokati.invoice.service.CarrierRatesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/carrier-rates")
@Tag(name = "Carrier Rates API", description = "Manage carrier rates")
public class CarrierRatesController {
    private static final Logger log = LoggerFactory.getLogger(CarrierRatesController.class);
    private final CarrierRatesService service;

    public CarrierRatesController(CarrierRatesService service) {
        this.service = service;
    }

    /**
     * GET: mirrors Node GET — findById(projectId) and return {} if not found.
     */
    @Operation(summary = "Get carrier rates by carrierProjectId")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getByCarrierProjectId(@RequestParam(required = false) String carrierProjectId) {
        log.info("Request getByProjectId : {}", carrierProjectId);
        if (carrierProjectId == null || carrierProjectId.isBlank() || "null".equals(carrierProjectId)) {
            return ResponseUtil.ok(Map.of(), "OK");
        }

        var maybe = service.findById(carrierProjectId); // Optional<CarrierRates>
        if (maybe.isEmpty()) {
            return ResponseUtil.ok(Map.of(), "OK"); // return {} in data
        }
        return ResponseUtil.ok(maybe.get(), "Carrier rates fetched successfully");
    }

    /**
     * POST: findByIdAndUpdate({ _id:id }, payload, { upsert:true, returnDocument:"after" })
     * Returns updated doc with message "Tarif aktualisiert".
     */
    @Operation(summary = "Upsert carrier rates (POST upsert)")
    @PostMapping
    public ResponseEntity<ApiResponse<CarrierRates>> create(@Valid @RequestBody CarrierRatesRequestDTO request) {
        log.info("Request create : {}", request);

        final String id = request.getCarrierProjectId();
        if (id == null || id.isBlank()) {
            // Let global exception handler return standardized error
            throw new com.jokati.invoice.exception.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_ARGUMENT",
                    "carrierProjectId is required"
            );
        }

        CarrierRates entity = CarrierRates.builder()
                .id(id)
                .carrierProjectId(id)
                .projectId(request.getProjectId())
                .rates(request.getRates())
                .payload(request.getPayload())
                .build();

        CarrierRates saved = service.upsert(id, entity);
        return ResponseUtil.ok(saved, "Tarif aktualisiert");
    }

    /**
     * PUT: findByIdAndUpdate(carrierProjectId, payload, { upsert:true, returnDocument:"after" })
     * Returns updated doc with message "Tarif aktualisiert".
     */
    @Operation(summary = "Upsert carrier rates (PUT upsert)")
    @PutMapping
    public ResponseEntity<ApiResponse<CarrierRates>> update(@Valid @RequestBody CarrierRatesRequestDTO request) {
        log.info("Request update : {}", request);

        final String id = request.getCarrierProjectId();
        if (id == null || id.isBlank()) {
            throw new com.jokati.invoice.exception.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_ARGUMENT",
                    "carrierProjectId is required"
            );
        }

        CarrierRates entity = CarrierRates.builder()
                .id(id)
                .carrierProjectId(id)
                .projectId(request.getProjectId())
                .rates(request.getRates())
                .payload(request.getPayload())
                .build();

        CarrierRates saved = service.upsert(id, entity);
        return ResponseUtil.ok(saved, "Tarif aktualisiert");
    }

    /**
     * DELETE: placeholder to mirror Node file (no-op body).
     */
    @Operation(summary = "Delete carrier rates by id (optional)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String id) {
        log.info("Request delete : {}", id);
        service.deleteById(id);
        // Node-style: 200 OK with message and {} in data
        return ResponseUtil.ok(Map.of(), "Carrier rates deleted (if existed)");
    }
}
