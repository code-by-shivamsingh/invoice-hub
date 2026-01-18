
package com.jokati.invoice.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.InvoiceListItemDTO;
import com.jokati.invoice.dto.InvoiceRequestDTO;
import com.jokati.invoice.dto.InvoiceResponseDTO;
import com.jokati.invoice.dto.ShipmentDTO;
import com.jokati.invoice.service.InvoiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Invoices")
@RestController
@RequestMapping("/api/v1/companies/{companyId}/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService service;

    @Operation(summary = "Create invoice for company (unique by companyId + invoiceNumber)")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(
            @PathVariable String companyId,
            @Valid @RequestBody InvoiceRequestDTO request) {
        log.info("Invoice create request recived : companyId={}, invoiceNumber={}", companyId, request.getInvoiceNumber());
        log.debug("Invoice create request recived : companyId={}, invoiceNumber={} request is :{}", companyId, request.getInvoiceNumber(),request);
        InvoiceResponseDTO dto = service.create(companyId, request);
        return ResponseUtil.okObject(dto, "Invoice created successfully");
    }

    @Operation(summary = "Get invoice by invoiceNumber for company")
    @GetMapping("/{invoiceNumber}")
    public ResponseEntity<ApiResponse<Object>> get(
            @PathVariable String companyId,
            @PathVariable String invoiceNumber) {
        log.info("Invoice get: companyId={}, invoiceNumber={}", companyId, invoiceNumber);
        InvoiceResponseDTO dto = service.get(companyId, invoiceNumber);
        return ResponseUtil.okObject(dto, "Invoice fetched successfully");
    }

    @Operation(summary = "Filtered list for UI (Carrier, Invoice Number, From/To date)")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> listFiltered(
            @PathVariable String companyId,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        log.info("Invoice listFiltered: companyId={}, carrier={}, invoiceNumber={}, from={}, to={}",
                companyId, carrier, invoiceNumber, fromDate, toDate);

        List<InvoiceListItemDTO> list = service.listFiltered(companyId, carrier, invoiceNumber, fromDate, toDate);
        return ResponseUtil.okObject(list, "Invoices fetched successfully");
    }

    @Operation(summary = "Replace invoice (PUT) by invoiceNumber for company")
    @PutMapping("/{invoiceNumber}")
    public ResponseEntity<ApiResponse<Object>> replace(
            @PathVariable String companyId,
            @PathVariable String invoiceNumber,
            @Valid @RequestBody InvoiceRequestDTO request) {
        log.info("Invoice replace: companyId={}, invoiceNumber={}", companyId, invoiceNumber);
        InvoiceResponseDTO dto = service.replace(companyId, invoiceNumber, request);
        return ResponseUtil.okObject(dto, "Invoice replaced successfully");
    }

    @Operation(summary = "Delete invoice by invoiceNumber for company")
    @DeleteMapping("/{invoiceNumber}")
    public ResponseEntity<ApiResponse<Object>> delete(
            @PathVariable String companyId,
            @PathVariable String invoiceNumber) {
        log.info("Invoice delete: companyId={}, invoiceNumber={}", companyId, invoiceNumber);
        service.delete(companyId, invoiceNumber);
        // Consistent with your other controllers: 200 with {} and message in envelope
        return ResponseUtil.okEmpty("Invoice deleted successfully");
    }

    @Operation(summary = "Add/replace a shipment in the invoice by shipmentId")
    @PostMapping("/{invoiceNumber}/shipments")
    public ResponseEntity<ApiResponse<Object>> addShipment(
            @PathVariable String companyId,
            @PathVariable String invoiceNumber,
            @Valid @RequestBody ShipmentDTO dto) {
        log.info("Invoice addShipment: companyId={}, invoiceNumber={}, shipmentId={}",
                companyId, invoiceNumber, dto.getShipmentId());
        InvoiceResponseDTO response = service.addShipment(companyId, invoiceNumber, dto);
        return ResponseUtil.okObject(response, "Shipment added to invoice successfully");
    }
}
