
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.InvoiceListItemDTO;
import com.jokati.invoice.dto.InvoiceRequestDTO;
import com.jokati.invoice.dto.InvoiceResponseDTO;
import com.jokati.invoice.dto.ShipmentDTO;
import com.jokati.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Invoices")
@RestController
@RequestMapping("/api/v1/companies/{companyId}/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;

    @Operation(summary = "Create invoice for company (unique by companyId + invoiceNumber)")
    @PostMapping
    public ResponseEntity<InvoiceResponseDTO> create(
            @PathVariable String companyId,
            @Valid @RequestBody InvoiceRequestDTO request) throws Exception {
        return ResponseEntity.ok(service.create(companyId, request));
    }

    @Operation(summary = "Get invoice by invoiceNumber for company")
    @GetMapping("/{invoiceNumber}")
    public ResponseEntity<InvoiceResponseDTO> get(
            @PathVariable String companyId,
            @PathVariable String invoiceNumber) throws Exception {
        return ResponseEntity.ok(service.get(companyId, invoiceNumber));
    }

    @Operation(summary = "Filtered list for UI (Carrier, Invoice Number, From/To date)")
    @GetMapping
    public ResponseEntity<List<InvoiceListItemDTO>> listFiltered(
            @PathVariable String companyId,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String fromDate, // supports yyyy-MM-dd or dd/MM/yyyy
            @RequestParam(required = false) String toDate     // supports yyyy-MM-dd or dd/MM/yyyy
    ) {
        return ResponseEntity.ok(
            service.listFiltered(companyId, carrier, invoiceNumber, fromDate, toDate)
        );
    }

    @Operation(summary = "Replace invoice (PUT) by invoiceNumber for company")
    @PutMapping("/{invoiceNumber}")
    public ResponseEntity<InvoiceResponseDTO> replace(
            @PathVariable String companyId,
            @PathVariable String invoiceNumber,
            @Valid @RequestBody InvoiceRequestDTO request) throws Exception {
        return ResponseEntity.ok(service.replace(companyId, invoiceNumber, request));
    }

    @Operation(summary = "Delete invoice by invoiceNumber for company")
    @DeleteMapping("/{invoiceNumber}")
    public ResponseEntity<Void> delete(
            @PathVariable String companyId,
            @PathVariable String invoiceNumber) throws Exception {
        service.delete(companyId, invoiceNumber);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add/replace a shipment in the invoice by shipmentId")
    @PostMapping("/{invoiceNumber}/shipments")
    public ResponseEntity<InvoiceResponseDTO> addShipment(
            @PathVariable String companyId,
            @PathVariable String invoiceNumber,
            @Valid @RequestBody ShipmentDTO dto) throws Exception {
        return ResponseEntity.ok(service.addShipment(companyId, invoiceNumber, dto));
    }
}
