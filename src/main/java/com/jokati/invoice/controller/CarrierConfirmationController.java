
package com.jokati.invoice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.CarrierAddressDTO;
import com.jokati.invoice.dto.CarrierConfirmationRequestDTO;
import com.jokati.invoice.model.CarrierConfirmation;
import com.jokati.invoice.service.CarrierConfirmationService;
import com.jokati.invoice.service.CarrierUserService;
import com.jokati.invoice.service.EmailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/carrier-confirmation")
@Tag(name = "Carrier Confirmation API", description = "Create/Update and fetch carrier confirmation")
public class CarrierConfirmationController {

    private static final Logger log = LoggerFactory.getLogger(CarrierConfirmationController.class);

    private final CarrierConfirmationService service;
    private final CarrierUserService carrierUserService;
    private final EmailService mailService;

    public CarrierConfirmationController(CarrierConfirmationService service,
                                         CarrierUserService carrierUserService,
                                         EmailService mailService) {
        this.service = service;
        this.carrierUserService = carrierUserService;
        this.mailService = mailService;
    }

    @Operation(summary = "Get carrier confirmation by projectId")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> getByProjectId(@RequestParam(required = false) String projectId) {
        log.info("Request getByProjectId : {}", projectId);

        if (projectId == null || projectId.isBlank() || "null".equalsIgnoreCase(projectId)) {
            // Node-style: 200 OK with {}
            return ResponseUtil.okEmpty("OK");
        }

        var maybe = service.findById(projectId);
        if (maybe.isEmpty()) {
            // Node-style: 200 OK with {}
            return ResponseUtil.okEmpty("OK");
        }

        // Return raw document in data
        return ResponseUtil.okObject(maybe.get(), "Carrier confirmation fetched successfully");
    }

    @Operation(summary = "Upsert carrier confirmation and optionally notify carriers")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> create(@RequestBody CarrierConfirmationRequestDTO request) {
        log.info("Request upsert : {}", request);

        final String id = request.getCarrierProjectId();
        if (id == null || id.isBlank()) {
            // Global handler will convert IllegalArgumentException -> 400 envelope
            throw new IllegalArgumentException("carrierProjectId is required");
        }

        CarrierConfirmation doc = CarrierConfirmation.builder()
                .id(id)
                .shipperProjectId(request.getShipperProjectId())
                .shipperCompany(request.getShipperCompany())
                .shipperProjectName(request.getShipperProjectName())
                .extra(request.getExtra())
                .build();

        CarrierConfirmation saved = service.upsert(id, doc);

        if (Boolean.TRUE.equals(request.getSendToCarrier()) && request.getSelectedCarrierData() != null) {
            for (CarrierAddressDTO carrier : request.getSelectedCarrierData()) {
                String initialPassword = System.getenv("CARRIER_INITIAL_FIREBASE_PASSWORD"); // move to properties if needed
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

        return ResponseUtil.okObject(saved, "Carrier confirmation saved");
    }

    @Operation(summary = "Delete carrier confirmation by projectId (optional)")
    @DeleteMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String projectId) {
        log.info("Request delete : {}", projectId);
        service.deleteById(projectId);
        log.info("Successfully processed delete for carrier confirmation");
        // Node-style success: 200 OK with {} and message in envelope
        return ResponseUtil.okEmpty("Carrier confirmation deleted (if existed)");
    }
}
