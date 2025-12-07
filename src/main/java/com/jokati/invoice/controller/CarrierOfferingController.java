
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.CarrierOfferingRequestDTO;
import com.jokati.invoice.dto.CarrierOfferingResponseDTO;
import com.jokati.invoice.model.CarrierOffering;
import com.jokati.invoice.service.CarrierOfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/carrier-offering")
@Tag(name = "Carrier Offering", description = "Create/Query carrier offerings")
public class CarrierOfferingController {

    private final CarrierOfferingService service;

    public CarrierOfferingController(CarrierOfferingService service) {
        this.service = service;
    }

    /**
     * GET: mirrors Node GET — find({ projectId }) and return list; if empty -> {}
     */
    @Operation(summary = "Get carrier offerings by projectId")
    @GetMapping(params = "projectId")
    public ResponseEntity<?> getByProjectId(@RequestParam String projectId) {
        log.info("GET carrier-offering by projectId={}", projectId);
        if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
            return ResponseEntity.ok(Map.of());
        }
        List<CarrierOffering> result = service.findByProjectId(projectId);
        if (result == null || result.isEmpty()) {
            return ResponseEntity.ok(Map.of()); // mimic Node's {} when "no result"
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST: upsert by carrierProjectId and return updated doc (returnDocument: "after")
     */
    @Operation(summary = "Create/Upsert carrier offering (POST)", description = "Upserts by carrierProjectId; _id set from carrierProjectId (ObjectId) on insert.")
    @PostMapping
    public ResponseEntity<CarrierOfferingResponseDTO> upsertPost(@RequestBody CarrierOfferingRequestDTO request) {
        log.info("POST carrier-offering upsert: {}", request);
        try {
            CarrierOffering updated = service.upsertByCarrierProjectId(request);
            return ResponseEntity.ok(CarrierOfferingResponseDTO.builder()
                    .message("Angebot gespeichert")
                    .carrierOffering(updated)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(CarrierOfferingResponseDTO.builder()
                    .message("Speichern des neuen Angebotes fehlgeschlagen")
                    .carrierOffering(null)
                    .build());
        }
    }

    /**
     * PUT: same behavior as POST (Node uses same handler)
     */
    @Operation(summary = "Create/Upsert carrier offering (PUT)", description = "Upserts by carrierProjectId; _id set from carrierProjectId (ObjectId) on insert.")
    @PutMapping
    public ResponseEntity<CarrierOfferingResponseDTO> upsertPut(@RequestBody CarrierOfferingRequestDTO request) {
        log.info("PUT carrier-offering upsert: {}", request);
        try {
            CarrierOffering updated = service.upsertByCarrierProjectId(request);
            return ResponseEntity.ok(CarrierOfferingResponseDTO.builder()
                    .message("Angebot gespeichert")
                    .carrierOffering(updated)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(CarrierOfferingResponseDTO.builder()
                    .message("Speichern des neuen Angebotes fehlgeschlagen")
                    .carrierOffering(null)
                    .build());
        }
    }

    /**
     * DELETE: placeholder — Node handler is empty
     */
    @Operation(summary = "Delete carrier offering by id (optional)")
    @DeleteMapping("/{id}")
    public ResponseEntity<CarrierOfferingResponseDTO> delete(@PathVariable String id) {
        log.info("DELETE carrier-offering id={}", id);
        service.deleteById(id);
        return ResponseEntity.ok(CarrierOfferingResponseDTO.builder()
                .message("Carrier offering gelöscht (falls vorhanden)")
                .carrierOffering(null)
                .build());
    }
}
