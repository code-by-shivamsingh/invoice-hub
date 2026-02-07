package com.jokati.invoice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.InvoiceCommunicationMessageRequestDTO;
import com.jokati.invoice.dto.InvoiceCommunicationResponseDTO;
import com.jokati.invoice.service.InvoiceCommunicationService;
import com.jokati.invoice.util.TextSanitizer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Invoice Carrier Communication")
@RestController
@RequestMapping("/api/v1/invoice-communications")
@RequiredArgsConstructor
@Slf4j
public class InvoiceCommunicationController {

    private final InvoiceCommunicationService service;

    @Operation(summary = "Send message (auto-create thread on first shipper message)")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> sendMessage(
            @Valid @RequestBody InvoiceCommunicationMessageRequestDTO request) {

        log.info("InvoiceCommunication POST: invoiceId={}, senderType={}, senderId={}, senderName={}",
                TextSanitizer.normalizeId(request.getInvoiceId()),
                request.getSenderType(),
                TextSanitizer.normalizeId(request.getSenderId()),
                request.getSenderName());

        InvoiceCommunicationResponseDTO response = service.sendMessage(request);
        return ResponseUtil.okObject(response, "Message sent successfully");
    }

    @Operation(summary = "Get conversation by invoiceId (latest message first)")
    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<Object>> getByInvoiceId(@PathVariable @NotBlank String invoiceId) {

        final String iid = TextSanitizer.normalizeId(invoiceId);
        log.info("InvoiceCommunication GET: invoiceId={}", iid);

        InvoiceCommunicationResponseDTO response = service.getByInvoiceId(iid);
        return ResponseUtil.okObject(response, "Invoice communication fetched successfully");
    }
}