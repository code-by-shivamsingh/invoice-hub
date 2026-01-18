
package com.jokati.invoice.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.jokati.invoice.dto.ChargesDTO;

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
public class Shipment {
	@NotBlank
	private String shipmentId;
	private LocalDate shipmentDate;
	@NotBlank
	private String originCountry;
	@NotBlank
	private String destinationCountry;

	@NotNull
	@Min(0)
	private Integer weightKg;

	// Optional strings (may be null or empty)
	private String zipCodeShipper; // "string|null"
	private String zipCodeConsignee; // "string|null"
	private String city; // "string|null"

	// Numbers as per your corrected structure (default to 0.0 in service layer if
	// needed)
	@NotNull
	@DecimalMin("0.0")
	private BigDecimal palletCount;


	private String packagingType;

	@NotNull
	@DecimalMin("0.0")
	private BigDecimal effectiveWeight;

	@NotNull
	@DecimalMin("0.0")
	private BigDecimal chargeableWeight;

	@NotNull
	@DecimalMin("0.0")
	private BigDecimal cubicMeters;

	@NotNull
	@DecimalMin("0.0")
	private BigDecimal loadingMeters;

	@NotNull
	@DecimalMin("0.0")
	private BigDecimal kilometer;

	@Valid
	@NotNull
	private Charges charges;

	@NotNull
	@DecimalMin("0.0")
	private BigDecimal totalPrice;

	/** NEW: order-side total for this shipment (optional) */
	@DecimalMin("0.0")
	private BigDecimal orderTotal;

	/** NEW: computed as orderTotal - netAmountEur; null when missing inputs */
	private BigDecimal difference;

	/** NEW: billing status as object (label + color) */
	private StatusInfo status;
}
