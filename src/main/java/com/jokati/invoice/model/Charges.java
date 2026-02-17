
package com.jokati.invoice.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Charges {
	@NotNull @DecimalMin("0.0")
    private BigDecimal freightCostSystem;

    @NotNull @DecimalMin("0.0")
    private BigDecimal dieselFee;

    @NotNull @DecimalMin("0.0")
    private BigDecimal tollCharge;

    @NotNull @DecimalMin("0.0")
    private BigDecimal expressNextDay;
    
    @NotNull @DecimalMin("0.0")
    private BigDecimal express12;
    
    @NotNull @DecimalMin("0.0")
    private BigDecimal express10;
    
    @NotNull @DecimalMin("0.0")
    private BigDecimal express8;

    @NotNull @DecimalMin("0.0")
    private BigDecimal palletExchange;

    @NotNull @DecimalMin("0.0")
    private BigDecimal phoneAvis;

    @NotNull @DecimalMin("0.0")
    private BigDecimal liftingPlatformSurcharge;
    
    @NotNull @DecimalMin("0.0")
    private BigDecimal tailLiftSurcharge;
    
    @NotNull @DecimalMin("0.0")
    private BigDecimal fixtermin;

    @NotNull @DecimalMin("0.0")
    private BigDecimal emailAvis;

    @NotNull @DecimalMin("0.0")
    private BigDecimal bookingInAvis;

    @NotNull @DecimalMin("0.0")
    private BigDecimal shortWeekSurcharge;
    
    @NotNull @DecimalMin("0.0")
    private BigDecimal insurance;

    @NotNull @DecimalMin("0.0")
    private BigDecimal dangerousGoodsSurcharge;

    @NotNull @DecimalMin("0.0")
    private BigDecimal securityFee;

    @NotNull @DecimalMin("0.0")
    private BigDecimal longGoodsSurcharge;
    
    @NotNull @DecimalMin("0.0")
    private BigDecimal portiPapiere;

    @NotNull @DecimalMin("0.0")
    private BigDecimal custom1;

    @NotNull @DecimalMin("0.0")
    private BigDecimal custom2;

    @NotNull @DecimalMin("0.0")
    private BigDecimal custom3;

    @NotNull @DecimalMin("0.0")
    private BigDecimal custom4;

    @NotNull @DecimalMin("0.0")
    private BigDecimal custom5;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;
}