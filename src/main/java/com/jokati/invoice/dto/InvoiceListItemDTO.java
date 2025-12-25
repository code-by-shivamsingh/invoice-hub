
package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceListItemDTO {

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @JsonProperty("invoice_date")  // ISO yyyy-MM-dd string
    private String invoiceDate;

    /** Carrier = seller.companyName (HTML unescaped) */
    @JsonProperty("carrier")
    private String carrier;

    /** Order total coming from order system (optional) */
    @JsonProperty("order_total")
    private BigDecimal orderTotal;

    /** Invoice total (totals.grossAmount) */
    @JsonProperty("invoice_total")
    private BigDecimal invoiceTotal;

    /** order_total - invoice_total; null when either missing */
    @JsonProperty("difference")
    private BigDecimal difference;

    /** "Correct billing" only when difference == 0 and order_total present; else "Incorrect billing" */
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("email_status")
    private String emailStatus;

    /** Include shipments in list response as requested */
    @JsonProperty("shipments")
    private List<ShipmentDTO> shipments;
}
