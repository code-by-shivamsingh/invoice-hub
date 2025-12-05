
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.CarrierOfferingRequestDTO;
import com.jokati.invoice.dto.CarrierOfferingResponseDTO;
import com.jokati.invoice.email.EmailTemplates;
import com.jokati.invoice.model.CarrierOffering;
import com.jokati.invoice.service.CarrierOfferingService;
import com.jokati.invoice.service.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carrier-offering")
@Tag(name = "Carrier Offering API", description = "Manage carrier offerings")
public class CarrierOfferingController {

    private final CarrierOfferingService service;
    private final MailService mailService;

    public CarrierOfferingController(CarrierOfferingService service, MailService mailService) {
        this.service = service;
        this.mailService = mailService;
    }

    /**
     * GET: mirrors Node GET — find({ projectId }) and return the array (or empty list).
     */
    @Operation(summary = "Get carrier offerings by projectId")
    @GetMapping
    public ResponseEntity<?> getByProjectId(@RequestParam(required = false) String projectId) {
        if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
            // Node returns {} when result falsy; but find() returns an array. We return [] when invalid.
            return ResponseEntity.ok(List.of());
        }

        List<CarrierOffering> result = service.findByProjectId(projectId);
        if (result == null || result.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * POST: findOneAndUpdate({ carrierProjectId }, { ...payload, _id:id }, { upsert:true, returnDocument:"after" })
     * Sends email when companyProfile.company present.
     */
    @Operation(summary = "Upsert carrier offering and notify shipper via email")
    @PostMapping
    public ResponseEntity<CarrierOfferingResponseDTO> upsert(@RequestBody CarrierOfferingRequestDTO request) {
        try {
            final String id = request.getCarrierProjectId();
            if (id == null || id.isBlank()) {
                return ResponseEntity.badRequest().body(CarrierOfferingResponseDTO.builder()
                        .message("carrierProjectId is required")
                        .data(null)
                        .build());
            }

            CarrierOffering entity = CarrierOffering.builder()
                    .id(id)
                    .carrierProjectId(id)
                    .projectId(request.getProjectId())
                    .shipperEmail(request.getShipperEmail())
                    .companyProfile(request.getCompanyProfile())
                    .payload(request.getPayload())
                    .build();

            CarrierOffering saved = service.upsert(id, entity);

            // Email sending: if companyProfile.company exists
            String subject = "Neues Angebot zur Ihrer Frachtausschreibung";
            String companyName = null;
            Map<String, Object> profile = request.getCompanyProfile();
            if (profile != null && profile.get("company") instanceof String s && !s.isBlank()) {
                companyName = s;
            }

            if (companyName != null && request.getShipperEmail() != null && !request.getShipperEmail().isBlank()) {
                String htmlBody = EmailTemplates.angebotErhalten(companyName);
                mailService.sendEmail(request.getShipperEmail(), subject, htmlBody);
            }

            return ResponseEntity.ok(CarrierOfferingResponseDTO.builder()
                    .message("Carrier offering saved successfully")
                    .data(saved)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(CarrierOfferingResponseDTO.builder()
                    .message("Speichern des neuen Angebotes fehlgeschlagen: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    /**
     * DELETE: placeholder to mirror Node file.
     */
    @Operation(summary = "Delete carrier offering by id (optional)")
    @DeleteMapping("/{id}")
    public ResponseEntity<CarrierOfferingResponseDTO> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.ok(CarrierOfferingResponseDTO.builder()
                .message("Carrier offering deleted (if existed)")
                .data(null)
                .build());
    }
}