package com.jokati.invoice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.CreateInvoiceCommunicationDTO;
import com.jokati.invoice.dto.CarrierResponseDTO;
import com.jokati.invoice.model.InvoiceCarrierCommunication;
import com.jokati.invoice.service.InvoiceCommunicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Invoice Carrier Communication")
@RestController
@RequestMapping("/api/invoice-communications")  
@RequiredArgsConstructor
public class InvoiceCommunicationController {

    private static final Logger log =
            LoggerFactory.getLogger(InvoiceCommunicationController.class);

    private final InvoiceCommunicationService service;

   
    @Operation(summary = "Send invoice to carrier and create communication")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(
            @RequestParam String companyId,   
            @RequestBody CreateInvoiceCommunicationDTO request) {

        log.info("InvoiceCommunication create: companyId={}, invoiceId={}",
                companyId, request.getInvoiceId());

        InvoiceCarrierCommunication response = service.create(request);
        return ResponseUtil.okObject(response, "Invoice sent to carrier successfully");
    }

    
    @Operation(summary = "Get invoice-carrier communication by invoiceId")
    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<Object>> getByInvoiceId(
            @RequestParam String companyId,   
            @PathVariable String invoiceId) {

        log.info("InvoiceCommunication get: companyId={}, invoiceId={}",
                companyId, invoiceId);

        InvoiceCarrierCommunication response =
                service.getByInvoiceId(invoiceId);

        return ResponseUtil.okObject(response, "Invoice communication fetched successfully");
    }

    
    @Operation(summary = "Carrier manual response (MANUALLY_ACCEPTED / MANUALLY_REJECTED)")
    @PostMapping("/{invoiceId}/carrier-response")
    public ResponseEntity<ApiResponse<Object>> carrierResponse(
            @RequestParam String companyId, 
            @PathVariable String invoiceId,
            @RequestBody CarrierResponseDTO request) {

        log.info("Carrier response: companyId={}, invoiceId={}, status={}",
                companyId, invoiceId, request.getStatus());

        InvoiceCarrierCommunication response =
                service.saveCarrierResponse(invoiceId, request);

        return ResponseUtil.okObject(response, "Carrier response saved successfully");
    }
}
