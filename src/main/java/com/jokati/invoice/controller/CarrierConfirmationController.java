
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.CarrierAddressDTO;
import com.jokati.invoice.dto.CarrierConfirmationRequestDTO;
import com.jokati.invoice.dto.CarrierConfirmationResponseDTO;
import com.jokati.invoice.model.CarrierConfirmation;
import com.jokati.invoice.service.CarrierConfirmationService;
import com.jokati.invoice.service.CarrierUserService;
import com.jokati.invoice.service.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/carrier-confirmation")
@Tag(name = "Carrier Confirmation API", description = "Create/Update and fetch carrier confirmation")
public class CarrierConfirmationController {

    private final CarrierConfirmationService service;
    private final CarrierUserService carrierUserService;
    private final MailService mailService;

    public CarrierConfirmationController(CarrierConfirmationService service,
                                         CarrierUserService carrierUserService,
                                         MailService mailService) {
        this.service = service;
        this.carrierUserService = carrierUserService;
        this.mailService = mailService;
    }

    @Operation(summary = "Get carrier confirmation by projectId")
    @GetMapping
    public ResponseEntity<?> getByProjectId(@RequestParam(required = false) String projectId) {
        if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
            // Node returns {} with 200 on not found/invalid
            return ResponseEntity.ok(Map.of());
        }

        var maybe = service.findById(projectId); // Optional<CarrierConfirmation>
        if (maybe.isEmpty()) {
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(maybe.get());
        // Alternatively:
        // return service.findById(projectId)
        //         .<ResponseEntity<?>>map(ResponseEntity::ok)
        //         .orElseGet(() -> ResponseEntity.ok(Map.of()));
    }

    @Operation(summary = "Upsert carrier confirmation and optionally notify carriers")
    @PostMapping
    public ResponseEntity<CarrierConfirmationResponseDTO> upsert(@RequestBody CarrierConfirmationRequestDTO request) {
        try {
            final String id = request.getCarrierProjectId();
            if (id == null || id.isBlank()) {
                return ResponseEntity.badRequest().body(CarrierConfirmationResponseDTO.builder()
                        .message("carrierProjectId is required")
                        .data(null)
                        .build());
            }

            CarrierConfirmation doc = CarrierConfirmation.builder()
                    .id(id)
                    .shipperProjectId(request.getShipperProjectId())
                    .shipperCompany(request.getShipperCompany())
                    .shipperProjectName(request.getShipperProjectName())
                    .build();

            doc.setExtra(request.getExtra());

            CarrierConfirmation saved = service.upsert(id, doc);

            if (Boolean.TRUE.equals(request.getSendToCarrier()) && request.getSelectedCarrierData() != null) {
                for (CarrierAddressDTO carrier : request.getSelectedCarrierData()) {
                    String initialPassword = System.getenv("CARRIER_INITIAL_FIREBASE_PASSWORD");
                    carrierUserService.createNewUser(carrier, initialPassword);

                    String subject = "Angebot erhalten";
                    String htmlBody = String.format(
                            "<p>Projekt: %s (%s)</p><p>Unternehmen: %s</p><p>Empfänger: %s</p>",
                            request.getShipperProjectName(),
                            request.getShipperProjectId(),
                            request.getShipperCompany(),
                            carrier.getEmail()
                    );
                    mailService.sendEmail(carrier.getEmail(), subject, htmlBody);
                }
            }

            return ResponseEntity.ok(CarrierConfirmationResponseDTO.builder()
                    .message("Carrier confirmation saved")
                    .data(saved)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(CarrierConfirmationResponseDTO.builder()
                    .message("Speichern der Bestätigung fehlgeschlagen: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @Operation(summary = "Delete carrier confirmation by projectId (optional)")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<CarrierConfirmationResponseDTO> delete(@PathVariable String projectId) {
        service.deleteById(projectId);
        return ResponseEntity.ok(CarrierConfirmationResponseDTO.builder()
                .message("Carrier confirmation deleted (if existed)")
                .data(null)
                .build());
    }
}
