package com.jokati.invoice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.CreateInvoiceCommunicationDTO;
import com.jokati.invoice.model.InvoiceCarrierCommunication;
import com.jokati.invoice.service.InvoiceCommunicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Invoice Carrier Communication")
@RestController
@RequestMapping("/api/v1/invoice-communications")
@RequiredArgsConstructor
public class InvoiceCommunicationController {

    private static final Logger log =
            LoggerFactory.getLogger(InvoiceCommunicationController.class);

    private final InvoiceCommunicationService service;

    @Operation(summary = "Send invoice to carrier and create communication")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(
            @Valid @RequestBody CreateInvoiceCommunicationDTO request) {

        log.info("InvoiceCommunication create: companyId={}, invoiceId={}, carrierName={}",
                request.getCompanyId(), request.getInvoiceId(), request.getCarrier());

        InvoiceCarrierCommunication response = service.create(request);
        return ResponseUtil.okObject(response, "Invoice sent to carrier successfully");
    }


    @Operation(summary = "Get invoice-carrier communication by invoiceId")
    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<Object>> getByInvoiceId(@PathVariable String invoiceId) {
        log.info("InvoiceCommunication get: invoiceId={}", invoiceId);
        InvoiceCarrierCommunication response = service.getByInvoiceId(invoiceId);
        return ResponseUtil.okObject(response, "Invoice communication fetched successfully");
    }
}
