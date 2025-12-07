package com.jokati.invoice.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.dto.ShipmentRequestDTO;
import com.jokati.invoice.dto.ShipmentResponseDTO;
import com.jokati.invoice.model.ShipmentData;
import com.jokati.invoice.service.ShipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/shipment")
@Tag(name = "Shipment API", description = "Operations related to shipment data")
public class ShipmentController {
	private static final Logger log = LoggerFactory.getLogger(ShipmentController.class);
    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @Operation(summary = "Get shipment data by projectId")
    @GetMapping
    public ResponseEntity<List<ShipmentData>> getShipmentData(@RequestParam String projectId) {
    	log.info("Request getShipmentData : {}",  projectId);
        if (projectId == null || projectId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.getShipmentData(projectId));
    }

    @Operation(summary = "Save shipment data batch")
    @PostMapping
    public ResponseEntity<ShipmentResponseDTO> saveShipmentData(@RequestBody ShipmentRequestDTO request) {
    	log.info("Request saveShipmentData : {}",  request);
    	if (request.getProjectId() == null || request.getShipmentData() == null || request.getShipmentData().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        request.getShipmentData().forEach(dto -> {
            ShipmentData entity = ShipmentData.builder()
                    .projectId(request.getProjectId())
                    .shipmentId(dto.getShipmentId())
                    .shipmentDate(dto.getShipmentDate())
                    .zipCodeShipper(dto.getZipCodeShipper())
                    .zipCodeConsignee(dto.getZipCodeConsignee())
                    .city(dto.getCity())
                    .country(dto.getCountry())
                    .length(dto.getLength())
                    .wide(dto.getWide())
                    .height(dto.getHeight())
                    .loadingMeters(dto.getLoadingMeters())
                    .cubicMeters(dto.getCubicMeters())
                    .palletCount(dto.getPalletCount())
                    .packagingType(dto.getPackagingType())
                    .effectiveWeight(dto.getEffectiveWeight())
                    .hasPackagingType(dto.getHasPackagingType())
                    .projectType(dto.getProjectType())
                    .expressNextDay(dto.getExpressNextDay())
                    .shortWeekSurcharge(dto.getShortWeekSurcharge())
                    .bookingAvis(dto.getBookingAvis())
                    .express12(dto.getExpress12())
                    .express10(dto.getExpress10())
                    .express8(dto.getExpress8())
                    .fixDate(dto.getFixDate())
                    .eMailAvis(dto.getEMailAvis())
                    .phoneAvis(dto.getPhoneAvis())
                    .dangerousGoodsSurcharge(dto.getDangerousGoodsSurcharge())
                    .carrierCertificate(dto.getCarrierCertificate())
                    .b2cSurchargeNational(dto.getB2cSurchargeNational())
                    .b2cSurchargeInternational(dto.getB2cSurchargeInternational())
                    .securityFee(dto.getSecurityFee())
                    .insurance(dto.getInsurance())
                    .porti(dto.getPorti())
                    .createdAt(LocalDateTime.now().toString())
                    .build();
            service.saveShipmentData(entity);
        });

        return ResponseEntity.ok(ShipmentResponseDTO.builder()
                .message("Batch saved successfully")
                .batchSize(request.getShipmentData().size())
                .build());
    }

    @Operation(summary = "Delete shipment data by projectId")
    @DeleteMapping
    public ResponseEntity<ShipmentResponseDTO> deleteShipmentData(@RequestParam String projectId) {
    	log.info("Request deleteShipmentData : {}",  projectId);
        if (projectId == null || projectId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        service.deleteByProjectId(projectId);
        return ResponseEntity.ok(ShipmentResponseDTO.builder()
                .message("Deleted all records for projectId: " + projectId)
                .batchSize(0)
                .build());
    }
}

