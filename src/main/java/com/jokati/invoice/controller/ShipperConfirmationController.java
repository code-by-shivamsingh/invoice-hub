
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.CarrierAddressDTO;
import com.jokati.invoice.dto.ShipperConfirmationRequestDTO;
import com.jokati.invoice.dto.ShipperConfirmationResponseDTO;
import com.jokati.invoice.email.EmailTemplates;
import com.jokati.invoice.model.ShipperConfirmation;
import com.jokati.invoice.service.JokatiUserService;
import com.jokati.invoice.service.MailService;
import com.jokati.invoice.service.ShipperConfirmationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shipper-confirmation")
@Tag(name = "Shipper Confirmation API", description = "Create/Update and fetch shipper confirmation")
public class ShipperConfirmationController {

    private final ShipperConfirmationService service;
    private final JokatiUserService jokatiUserService;
    private final MailService mailService;

    public ShipperConfirmationController(ShipperConfirmationService service,
                                         JokatiUserService jokatiUserService,
                                         MailService mailService) {
        this.service = service;
        this.jokatiUserService = jokatiUserService;
        this.mailService = mailService;
    }

    /**
     * GET: mirrors Node GET — fetch by projectId and return {} if not found.
     */
    @Operation(summary = "Get shipper confirmation by projectId")
    @GetMapping
    public ResponseEntity<?> getByProjectId(@RequestParam(required = false) String projectId) {
        if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
            return ResponseEntity.ok(Map.of());
        }

        var maybe = service.findById(projectId); // Optional<ShipperConfirmation>
        if (maybe.isEmpty()) {
            return ResponseEntity.ok(Map.of()); // {}
        }
        return ResponseEntity.ok(maybe.get());

        // Alternative functional style (also valid):
        // return service.findById(projectId)
        //         .<ResponseEntity<?>>map(ResponseEntity::ok)
        //         .orElseGet(() -> ResponseEntity.ok(Map.of()));
    }

    /**
     * POST: upsert by shipperProjectId and optionally notify carriers (create users + send email).
     * Mirrors Node:
     *   findOneAndUpdate({ _id: id }, payload, { upsert: true, returnDocument: "after" })
     */
    @Operation(summary = "Upsert shipper confirmation and optionally invite carriers")
    @PostMapping
    public ResponseEntity<ShipperConfirmationResponseDTO> upsert(@RequestBody ShipperConfirmationRequestDTO request) {
        try {
            final String id = request.getShipperProjectId();
            if (id == null || id.isBlank()) {
                return ResponseEntity.status(401).body(ShipperConfirmationResponseDTO.builder()
                        .message("shipperProjectId is required")
                        .success(false)
                        .confirmation(null)
                        .error("ValidationError")
                        .build());
            }

            ShipperConfirmation doc = ShipperConfirmation.builder()
                    .id(id)
                    .shipperProjectId(id)
                    .shipperCompany(request.getShipperCompany())
                    .shipperProjectName(request.getShipperProjectName())
                    .extra(request.getExtra())
                    .build();

            ShipperConfirmation saved = service.upsert(id, doc);

            // if sendToCarrier, register users + send email
            if (Boolean.TRUE.equals(request.getSendToCarrier()) && request.getSelectedCarrierData() != null) {
                final String subject = "Einladung zur Teilnahme an einer Frachtausschreibung - Angebotsabgabe erbeten";
                final String initialPassword = System.getenv("CARRIER_INITIAL_FIREBASE_PASSWORD");

                for (CarrierAddressDTO carrier : request.getSelectedCarrierData()) {
                    // Create user (stubbed)
                    jokatiUserService.createNewUser(carrier, initialPassword);

                    // Build and send email
                    String htmlBody = EmailTemplates.ausschreibung(
                            request.getShipperProjectId(),
                            request.getShipperCompany(),
                            request.getShipperProjectName(),
                            carrier.getEmail()
                    );
                    mailService.sendEmail(carrier.getEmail(), subject, htmlBody);
                }
            }

            return ResponseEntity.ok(ShipperConfirmationResponseDTO.builder()
                    .message("Bestätigungsdaten erfolgreich übermittelt")
                    .success(true)
                    .confirmation(saved)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(ShipperConfirmationResponseDTO.builder()
                    .message("Verbindung zum Server fehlgeschlagen.")
                    .success(false)
                    .confirmation(null)
                    .error(e.getMessage())
                    .build());
        }
    }

    /**
     * DELETE: placeholder (Node handler is empty).
     */
    @Operation(summary = "Delete shipper confirmation by projectId (optional)")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ShipperConfirmationResponseDTO> delete(@PathVariable String projectId) {
        service.deleteById(projectId);
        return ResponseEntity.ok(ShipperConfirmationResponseDTO.builder()
                .message("Shipper confirmation deleted (if existed)")
                .success(true)
                .confirmation(null)
                .build());
    }
}
