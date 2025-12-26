
package com.jokati.invoice.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceRequestDTO {
    /** Must match path {companyId} */
    @NotBlank private String companyId;

    @JsonProperty("document_type")  @NotBlank private String documentType;
    @JsonProperty("invoice_number") @NotBlank private String invoiceNumber;
    @JsonProperty("invoice_date")   @NotBlank private String invoiceDate; // yyyy-MM-dd
    @JsonProperty("due_date")       @NotBlank private String dueDate;
    @JsonProperty("currency")       @Pattern(regexp = "^[A-Z]{3}$") private String currency;

    @Valid @JsonProperty("seller")           private PartyDTO seller;
    @Valid @JsonProperty("bill_to")          private PartyDTO billTo;
    @Valid @JsonProperty("totals")           private TotalsDTO totals;
    @Valid @JsonProperty("shipment_summary") private ShipmentSummaryDTO shipmentSummary;

    @Valid @JsonProperty("shipments")        private List<ShipmentDTO> shipments;

    @JsonProperty("extraction_scope")        @NotBlank private String extractionScope;

    /** NEW: Order total & status (optional in request; status can be computed) */
    @JsonProperty("order_total")             private BigDecimal orderTotal;
    @JsonProperty("status")                  private String status;
    @JsonProperty("email_status")            private String emailStatus;
}
