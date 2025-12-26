
package com.jokati.invoice.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Shipment {
    @NotBlank private String shipmentId;
    @NotNull  private LocalDate pickupDate;
    @NotBlank private String originCountry;
    @NotBlank private String destinationCountry;
    @NotNull @DecimalMin("0.0") private BigDecimal weightKg;

    @Valid @NotNull private Charges charges;

    /** Invoice-side total for this shipment */
    @NotNull @DecimalMin("0.0") private BigDecimal netAmountEur;

    /** NEW: order-side total for this shipment (optional) */
    @DecimalMin("0.0") private BigDecimal orderTotal;

    /** NEW: computed as orderTotal - netAmountEur; null when missing inputs */
    private BigDecimal difference;

    /** NEW: "Correct billing" when orderTotal exists and difference == 0, else "Incorrect billing" */
    private String status;
}
