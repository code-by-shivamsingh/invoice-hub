
package com.jokati.invoice.controller;

import java.util.List;
import java.util.Map;

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
import com.jokati.invoice.dto.CarrierOfferingRequestDTO;
import com.jokati.invoice.model.CarrierOffering;
import com.jokati.invoice.service.CarrierOfferingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/carrier-offering")
@Tag(name = "Carrier Offering", description = "Create/Query carrier offerings")
@RequiredArgsConstructor
public class CarrierOfferingController {

    private final CarrierOfferingService service;

    /**
     * GET: mirrors Node GET — find({ projectId }) and return list; if empty -> {}
     */
    @Operation(summary = "Get carrier offerings by projectId")
    @GetMapping(params = "projectId")
    public ResponseEntity<ApiResponse<Object>> getByProjectId(@RequestParam String projectId) {
        log.info("GET carrier-offering by projectId={}", projectId);
        if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
            // Node-style: return {} with 200
            return ResponseUtil.ok(Map.of(), "OK");
        }
        List<CarrierOffering> result = service.findByProjectId(projectId);
        if (result == null || result.isEmpty()) {
            return ResponseUtil.ok(Map.of(), "OK");
        }
        return ResponseUtil.ok(result, "Carrier offerings fetched successfully");
    }

    /**
     * POST: upsert by carrierProjectId and return updated doc (returnDocument: "after")
     */
    @Operation(summary = "Create/Upsert carrier offering (POST)",
               description = "Upserts by carrierProjectId; _id set from carrierProjectId (ObjectId) on insert.")
    @PostMapping
    public ResponseEntity<ApiResponse<CarrierOffering>> upsertPost(@Valid @RequestBody CarrierOfferingRequestDTO request) {
        log.info("POST carrier-offering upsert");
        CarrierOffering updated = service.upsertByCarrierProjectId(request);
        return ResponseUtil.ok(updated, "Angebot gespeichert");
    }

    /**
     * PUT: same behavior as POST (Node uses same handler)
     */
    @Operation(summary = "Create/Upsert carrier offering (PUT)",
               description = "Upserts by carrierProjectId; _id set from carrierProjectId (ObjectId) on insert.")
    @PutMapping
    public ResponseEntity<ApiResponse<CarrierOffering>> upsertPut(@Valid @RequestBody CarrierOfferingRequestDTO request) {
        log.info("PUT carrier-offering upsert");
        CarrierOffering updated = service.upsertByCarrierProjectId(request);
        return ResponseUtil.ok(updated, "Angebot gespeichert");
    }

    /**
     * DELETE: placeholder — Node handler is empty
     */
    @Operation(summary = "Delete carrier offering by id (optional)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String id) {
        log.info("DELETE carrier-offering id={}", id);
        service.deleteById(id);
        // Node-style: always 200 OK with a message; data can be {}
        return ResponseUtil.ok(Map.of(), "Carrier offering gelöscht (falls vorhanden)");
    }
}
