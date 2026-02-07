
package com.jokati.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jokati.invoice.model.StatusInfo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDTO {

    @NotBlank
    private String shipmentId;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate shipmentDate;

    @NotBlank
    private String originCountry;

    @NotBlank
    private String destinationCountry;

    @NotNull
    @Min(0)
    private Integer weightKg;

    // Optional strings (may be null or empty)
    private String zipCodeShipper;   // "string|null"
    private String zipCodeConsignee; // "string|null"
    private String city;             // "string|null"

    // Numbers as per your corrected structure (default to 0.0 in service layer if needed)
    @NotNull @DecimalMin("0.0")
    private BigDecimal palletCount;

    
    private String packagingType;

    @NotNull @DecimalMin("0.0")
    private BigDecimal effectiveWeight;

    @NotNull @DecimalMin("0.0")
    private BigDecimal chargeableWeight;

    @NotNull @DecimalMin("0.0")
    private BigDecimal cubicMeters;

    @NotNull @DecimalMin("0.0")
    private BigDecimal loadingMeters;

    @NotNull @DecimalMin("0.0")
    private BigDecimal kilometer;

    @NotNull
    @Valid
    private ChargesDTO charges;
    private ChargesDTO orderCharges;
    private BigDecimal orderSurchargeTotal;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal totalPrice;


	/** NEW: order-side total for this shipment (optional) */
	@JsonProperty("order_total")
	@DecimalMin("0.0")
	private BigDecimal orderTotal;

	/** NEW: computed field; sent in responses */
	@JsonProperty("difference")
	private BigDecimal difference;

	/** NEW: computed field; sent in responses */
	@JsonProperty("status")
	private StatusInfo status;
}
