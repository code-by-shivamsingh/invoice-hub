package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

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
    private PartyDTO seller;

    @NotNull @Valid
    @JsonProperty("bill_to")
    private PartyDTO billTo;

    @NotNull @Valid
    private TotalsDTO totals;

    @NotNull @Valid
    @JsonProperty("shipment_summary")
    private ShipmentSummaryDTO shipmentSummary;

    @NotNull @Valid
    private List<ShipmentDTO> shipments;
}
