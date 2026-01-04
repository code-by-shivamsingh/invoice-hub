
package com.jokati.invoice.controller;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ErrorCodes;
import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.CarrierAddressDTO;
import com.jokati.invoice.dto.ShipperConfirmationRequestDTO;
import com.jokati.invoice.model.ShipperConfirmation;
import com.jokati.invoice.service.EmailService;
import com.jokati.invoice.service.JokatiUserService;
import com.jokati.invoice.service.ShipperConfirmationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/shipper-confirmation")
@Tag(name = "Shipper Confirmation API", description = "Create/Update and fetch shipper confirmation")
@RequiredArgsConstructor
public class ShipperConfirmationController {

    private static final Logger log = LoggerFactory.getLogger(ShipperConfirmationController.class);

    private final ShipperConfirmationService service;
    private final JokatiUserService jokatiUserService;
    private final EmailService mailService;

    /**
     * GET: mirrors Node GET — fetch by projectId and return {} if not found.
     */
    @Operation(summary = "Get shipper confirmation by projectId")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getByProjectId(@RequestParam(required = false) String projectId) {
        log.info("Request getByProjectId : {}", projectId);
        if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
            return ResponseUtil.okEmpty("OK");
        }

        var maybe = service.findById(projectId); // Optional<ShipperConfirmation>
        if (maybe.isEmpty()) {
            return ResponseUtil.okEmpty("OK"); // {}
        }
        return ResponseUtil.ok(maybe.get(), "Shipper confirmation fetched");
    }

    /**
     * POST: upsert by shipperProjectId and optionally notify carriers (create users + send email).
     * Mirrors Node:
     *   findOneAndUpdate({ _id: id }, payload, { upsert: true, returnDocument: "after" })
     */
    @Operation(summary = "Upsert shipper confirmation and optionally invite carriers")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(@RequestBody ShipperConfirmationRequestDTO request) {
        log.info("Request upsert : shipperProjectId={}, shipperCompany={}, shipperProjectName={}",
                request.getShipperProjectId(), request.getShipperCompany(), request.getShipperProjectName());

        final String id = request.getShipperProjectId();
        if (id == null || id.isBlank()) {
            return ResponseUtil.error(
                    HttpStatus.BAD_REQUEST,
                    "shipperProjectId is required",
                    new ErrorDetail(ErrorCodes.VALIDATION_ERROR, "shipperProjectId is required", "shipperProjectId", null)
            );
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
                if (carrier == null || carrier.getEmail() == null || carrier.getEmail().isBlank()) {
                    log.warn("Skipping carrier without valid email: {}", carrier);
                    continue;
                }

                // Create user (stubbed or actual implementation)
                jokatiUserService.createNewUser(carrier, initialPassword);

                // Build and send email (template static; consider moving to EmailTemplates bean if needed)
                String htmlBody = com.jokati.invoice.email.EmailTemplates.ausschreibung(
                        Objects.requireNonNullElse(request.getShipperProjectId(), ""),
                        Objects.requireNonNullElse(request.getShipperCompany(), ""),
                        Objects.requireNonNullElse(request.getShipperProjectName(), ""),
                        carrier.getEmail()
                );
                mailService.sendEmail(carrier.getEmail(), subject, htmlBody);
            }
        }

        return ResponseUtil.ok(saved, "Bestätigungsdaten erfolgreich übermittelt");
    }

    /**
     * DELETE: placeholder (Node handler is empty).
     */
    @Operation(summary = "Delete shipper confirmation by projectId (optional)")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String projectId) {
        log.info("Request delete : {}", projectId);
        service.deleteById(projectId);
        log.info("Successful delete shipper confirmation by projectId");
        // Node-style: return {} + message
        return ResponseUtil.okEmpty("Shipper confirmation deleted (if existed)");
    }
}
