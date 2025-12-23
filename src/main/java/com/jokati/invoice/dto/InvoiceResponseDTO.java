
package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceResponseDTO {
    private String id;
    private String companyId;

    @JsonProperty("document_type")  private String documentType;
    @JsonProperty("invoice_number") private String invoiceNumber;
    @JsonProperty("invoice_date")   private String invoiceDate;
    @JsonProperty("due_date")       private String dueDate;
    @JsonProperty("currency")       private String currency;

    @JsonProperty("seller")           private PartyDTO seller;
    @JsonProperty("bill_to")          private PartyDTO billTo;
    @JsonProperty("totals")           private TotalsDTO totals;
    @JsonProperty("shipment_summary") private ShipmentSummaryDTO shipmentSummary;

    @JsonProperty("shipments")        private List<ShipmentDTO> shipments;

    @JsonProperty("extraction_scope") private String extractionScope;

    /** NEW: also surface order total, computed carrier, difference & status */
    @JsonProperty("order_total")      private BigDecimal orderTotal;
    @JsonProperty("carrier")          private String carrier;
    @JsonProperty("invoice_total")    private BigDecimal invoiceTotal;
    @JsonProperty("difference")       private BigDecimal difference;
    @JsonProperty("status")           private String status;

    private Instant createdAt;
    private Instant updatedAt;
}
