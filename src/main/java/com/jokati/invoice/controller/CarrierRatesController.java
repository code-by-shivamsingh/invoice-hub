
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.CarrierRatesRequestDTO;
import com.jokati.invoice.dto.CarrierRatesResponseDTO;
import com.jokati.invoice.model.CarrierRates;
import com.jokati.invoice.service.CarrierRatesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/carrier-rates")
@Tag(name = "Carrier Rates API", description = "Manage carrier rates")
public class CarrierRatesController {

    private final CarrierRatesService service;

    public CarrierRatesController(CarrierRatesService service) {
        this.service = service;
    }

    /**
     * GET: mirrors Node GET — findById(projectId) and return {} if not found.
     */
    @Operation(summary = "Get carrier rates by projectId")
    @GetMapping
    public ResponseEntity<?> getByProjectId(@RequestParam(required = false) String projectId) {
        if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
            return ResponseEntity.ok(Map.of());
        }

        // Avoid type mismatch by not mixing ResponseEntity<CarrierRates> with ResponseEntity<Map>
        var maybe = service.findById(projectId); // Optional<CarrierRates>
        if (maybe.isEmpty()) {
            return ResponseEntity.ok(Map.of()); // return {}
        }
        return ResponseEntity.ok(maybe.get());
        // Alternative functional style:
        // return service.findById(projectId)
        //         .<ResponseEntity<?>>map(ResponseEntity::ok)
        //         .orElseGet(() -> ResponseEntity.ok(Map.of()));
    }

    /**
     * POST: findByIdAndUpdate({ _id:id }, payload, { upsert:true, returnDocument:"after" })
     * Returns updated doc with message "Tarif aktualisiert".
     */
    @Operation(summary = "Upsert carrier rates (POST upsert)")
    @PostMapping
    public ResponseEntity<CarrierRatesResponseDTO> postUpsert(@RequestBody CarrierRatesRequestDTO request) {
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
    public ResponseEntity<CarrierRatesResponseDTO> putUpsert(@RequestBody CarrierRatesRequestDTO request) {
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
        service.deleteById(id);
        return ResponseEntity.ok(CarrierRatesResponseDTO.builder()
                .message("Carrier rates deleted (if existed)")
                .carrierRates(null)
                .build());
    }
}
