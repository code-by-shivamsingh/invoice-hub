package com.jokati.invoice.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChargesDTO {

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal freightCostSystem = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal dieselFee = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal tollCharge = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal expressNextDay = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal express12 = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal express10 = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal express8 = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal palletExchange = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal phoneAvis = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal liftingPlatformSurcharge = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal tailLiftSurcharge = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal fixtermin = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal emailAvis = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal bookingInAvis = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal shortWeekSurcharge = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal insurance = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal dangerousGoodsSurcharge = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal securityFee = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal longGoodsSurcharge = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal portiPapiere = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal custom1 = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal custom2 = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal custom3 = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal custom4 = BigDecimal.ZERO;

    @Builder.Default  @DecimalMin("0.0")
    private BigDecimal custom5 = BigDecimal.ZERO;

    @Builder.Default @NotBlank @Size(min = 3, max = 3)
    private String currency = "EUR";
}