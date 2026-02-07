
package com.jokati.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequestDTO {
    /** Must match path {companyId} */
    @NotBlank
    private String companyId;

    @NotBlank
    private String carrier;

    @NotBlank
    private String invoiceNumber;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;

    @JsonProperty("currency")
    @Pattern(regexp = "^[A-Z]{3}$")
    private String currency;

    @Valid
    @JsonProperty("totals")
    private TotalsDTO totals;

    @Valid
    @JsonProperty("shipments")
    private List<ShipmentDTO> shipments;

    /** NEW: Order total & status (optional in request; status can be computed) */
    @JsonProperty("order_total")
    private BigDecimal orderTotal;

    @JsonProperty("email_status")
    private String emailStatus;
}