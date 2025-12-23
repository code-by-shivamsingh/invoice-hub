
package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShipmentDTO {
    @JsonProperty("shipment_id")        @NotBlank private String shipmentId;
    @JsonProperty("pickup_date")        @NotBlank private String pickupDate; // yyyy-MM-dd
    @JsonProperty("origin_country")     @NotBlank private String originCountry;
    @JsonProperty("destination_country")@NotBlank private String destinationCountry;
    @JsonProperty("weight_kg")          @NotNull @DecimalMin("0.0") private BigDecimal weightKg;

    @Valid @JsonProperty("charges")     @NotNull private ChargesDTO charges;

    @JsonProperty("net_amount_eur")     @NotNull @DecimalMin("0.0") private BigDecimal netAmountEur;

    /** NEW: order-side total for this shipment (optional) */
    @JsonProperty("order_total")        @DecimalMin("0.0") private BigDecimal orderTotal;

    /** NEW: computed field; sent in responses */
    @JsonProperty("difference")         private BigDecimal difference;

    /** NEW: computed field; sent in responses */
    @JsonProperty("status")             private String status;
}
