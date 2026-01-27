
package com.jokati.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jokati.invoice.model.StatusInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceResponseDTO {
    private String id;
    private String companyId;
    @JsonProperty("carrier")          private String carrier;
    @JsonProperty("invoice_number") private String invoiceNumber;
    @JsonProperty("invoice_date")   private String invoiceDate;
    @JsonProperty("due_date")       private String dueDate;
    @JsonProperty("currency")       private String currency;
    @JsonProperty("totals")           private TotalsDTO totals;

    @JsonProperty("shipments")        private List<ShipmentDTO> shipments;


    /** NEW: also surface order total, computed carrier, difference & status */
    @JsonProperty("order_total")      private BigDecimal orderTotal;
    
    @JsonProperty("invoice_total")    private BigDecimal invoiceTotal;
    @JsonProperty("difference")       private BigDecimal difference;
    @JsonProperty("status")           private StatusInfo status;
    @JsonProperty("email_status")     private Boolean emailStatus;
    
    
    private Instant createdAt;
    private Instant updatedAt;
}
