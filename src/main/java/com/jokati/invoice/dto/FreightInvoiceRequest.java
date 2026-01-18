package com.jokati.invoice.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreightInvoiceRequest {

    @NotNull
    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @NotNull
    @JsonProperty("invoice_date")
    private String invoiceDate;

    @NotNull
    @JsonProperty("due_date")
    private String dueDate;

    @NotNull
    private String currency;

    @NotNull @Valid
    private TotalsDTO totals;

    @NotNull @Valid
    @JsonProperty("shipment_summary")
    private ShipmentSummaryDTO shipmentSummary;

    @NotNull @Valid
    private List<ShipmentDTO> shipments;
}
