
package com.jokati.invoice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "invoices")
@CompoundIndex(name = "company_invoice_unique_idx", def = "{'companyId': 1, 'invoiceNumber': 1}", unique = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {
	@Id
    private String id;

    /** Company owner; replaces userId. */
    @Indexed
    @NotBlank
    private String companyId;
    
    @Indexed
    @NotBlank
    private String carrier;

    @Indexed
    @NotBlank
    private String invoiceNumber;
    
    @Indexed
    private LocalDate invoiceDate;



    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be ISO-4217 (3 uppercase letters)")
    private String currency;

    @Valid private Totals totals;


    @Builder.Default
    @Valid
    private List<Shipment> shipments = new ArrayList<>();



    /** NEW: order total coming from the order system (for comparison) */
    private BigDecimal orderTotal;

    private BigDecimal invoiceDifference;
    /** SYSTEM status (auto calculated) */
    private StatusInfo systemStatus;

    /** FINAL status (manual override) */
    private StatusInfo finalStatus;

    private Boolean emailStatus;

    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

}
