
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.CarrierRatesRequestDTO;
import com.jokati.invoice.dto.CarrierRatesResponseDTO;
import com.jokati.invoice.model.CarrierRates;
import com.jokati.invoice.service.CarrierRatesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<?> getByCarrierProjectId(@RequestParam(required = false) String carrierProjectId) {
    	log.info("Request getByProjectId : {}",  carrierProjectId);
        if (carrierProjectId == null || carrierProjectId.isBlank() || "null".equals(carrierProjectId)) {
            return ResponseEntity.ok(Map.of());
        }

        // Avoid type mismatch by not mixing ResponseEntity<CarrierRates> with ResponseEntity<Map>
        var maybe = service.findById(carrierProjectId); // Optional<CarrierRates>
        if (maybe.isEmpty()) {
            return ResponseEntity.ok(Map.of()); // return {}
        }
        return ResponseEntity.ok(maybe.get());
        
    }

    /**
     * POST: findByIdAndUpdate({ _id:id }, payload, { upsert:true, returnDocument:"after" })
     * Returns updated doc with message "Tarif aktualisiert".
     */
    @Operation(summary = "Upsert carrier rates (POST upsert)")
    @PostMapping
    public ResponseEntity<CarrierRatesResponseDTO> create(@RequestBody CarrierRatesRequestDTO request) {
    	log.info("Request create : {}",  request);
        try {
            final String id = request.getCarrierProjectId();
            if (id == null || id.isBlank()) {
                return ResponseEntity.badRequest().body(CarrierRatesResponseDTO.builder()
                        .message("carrierProjectId is required")
                        .carrierRates(null)
                        .build());
            }

            CarrierRates entity = CarrierRates.builder()
                    .id(id)
                    .carrierProjectId(id)
                    .projectId(request.getProjectId())
                    .rates(request.getRates())
                    .payload(request.getPayload())
                    .build();

            CarrierRates saved = service.upsert(id, entity);

            return ResponseEntity.ok(CarrierRatesResponseDTO.builder()
                    .message("Tarif aktualisiert")
                    .carrierRates(saved)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(CarrierRatesResponseDTO.builder()
                    .message("Tarife Speichern fehlgeschlagen: " + e.getMessage())
                    .carrierRates(null)
                    .build());
        }
    }

    /**
     * PUT: findByIdAndUpdate(carrierProjectId, payload, { upsert:true, returnDocument:"after" })
     * Returns updated doc with message "Tarif aktualisiert".
     */
    @Operation(summary = "Upsert carrier rates (PUT upsert)")
    @PutMapping
    public ResponseEntity<CarrierRatesResponseDTO> update(@RequestBody CarrierRatesRequestDTO request) {
    	log.info("Request update : {}",  request);
        try {
            final String id = request.getCarrierProjectId();
            if (id == null || id.isBlank()) {
                return ResponseEntity.badRequest().body(CarrierRatesResponseDTO.builder()
                        .message("carrierProjectId is required")
                        .carrierRates(null)
                        .build());
            }

            CarrierRates entity = CarrierRates.builder()
                    .id(id)
                    .carrierProjectId(id)
                    .projectId(request.getProjectId())
                    .rates(request.getRates())
                    .payload(request.getPayload())
                    .build();

            CarrierRates saved = service.upsert(id, entity);

            return ResponseEntity.ok(CarrierRatesResponseDTO.builder()
                    .message("Tarif aktualisiert")
                    .carrierRates(saved)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(CarrierRatesResponseDTO.builder()
                    .message("Verbindung zum Server fehlgeschlagen: " + e.getMessage())
                    .carrierRates(null)
                    .build());
        }
    }

    /**
     * DELETE: placeholder to mirror Node file (no-op body).
     */
    @Operation(summary = "Delete carrier rates by id (optional)")
    @DeleteMapping("/{id}")
    public ResponseEntity<CarrierRatesResponseDTO> delete(@PathVariable String id) {
    	log.info("Request delete : {}",  id);
        service.deleteById(id);
        return ResponseEntity.ok(CarrierRatesResponseDTO.builder()
                .message("Carrier rates deleted (if existed)")
                .carrierRates(null)
                .build());
    }
}
