
package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentItemResponseDTO {

    @JsonProperty("Id")
    private String idUpper;

    @JsonProperty("id")
    private String idLower;

    @JsonProperty("ShipmentId")
    private String shipmentId;

    @JsonProperty("ShipmentDate")
    private String shipmentDate; // yyyy-M-d or as stored

    @JsonProperty("ZipCodeShipper")
    private String zipCodeShipper;

    @JsonProperty("ZipCodeConsignee")
    private String zipCodeConsignee;

    @JsonProperty("City")
    private String city;

    @JsonProperty("Country")
    private String country;

    @JsonProperty("Length")
    private Double length;

    @JsonProperty("Wide")
    private Double wide;

    @JsonProperty("Height")
    private Double height;

    @JsonProperty("LoadingMeters")
    private Double loadingMeters;

    @JsonProperty("CubicMeters")
    private Double cubicMeters;

    @JsonProperty("PalletCount")
    private Integer palletCount;

    @JsonProperty("PackagingType")
    private String packagingType;

    @JsonProperty("EffectiveWeight")
    private Double effectiveWeight;

    @JsonProperty("ChargeableWeight")
    private Double chargeableWeight;

    @JsonProperty("MinimumWeight")
    private Double minimumWeight;

    @JsonProperty("WeightByCubicMeters")
    private Double weightByCubicMeters;

    @JsonProperty("WeightByLoadingMeters")
    private Double weightByLoadingMeters;

    @JsonProperty("Stackable")
    private Boolean stackable;

    @JsonProperty("StackFactor")
    private Integer stackFactor;

    @JsonProperty("StackId")
    private String stackId;

    @JsonProperty("StackFootprintLoadingMeters")
    private Double stackFootprintLoadingMeters;

    @JsonProperty("ExpressNextDay")
    private Boolean expressNextDay;

    @JsonProperty("Express12")
    private Boolean express12;

    @JsonProperty("Express10")
    private Boolean express10;

    @JsonProperty("Express8")
    private Boolean express8;

    @JsonProperty("Fixtermin")
    private Boolean fixtermin;

    @JsonProperty("EmailAvis")
    private Boolean emailAvis;

    @JsonProperty("PhoneAvis")
    private Boolean phoneAvis;

    @JsonProperty("BookingInAvis")
    private Boolean bookingInAvis;

    @JsonProperty("DangerousGoodsSurcharge")
    private Boolean dangerousGoodsSurcharge;

    @JsonProperty("LongGoodsSurcharge")
    private Boolean longGoodsSurcharge;

    @JsonProperty("ShortWeekSurcharge")
    private Boolean shortWeekSurcharge;

    @JsonProperty("PalletExchange")
    private Boolean palletExchange;

    @JsonProperty("PalletBoxExchange")
    private Boolean palletBoxExchange;

    @JsonProperty("CarrierCertificate")
    private Boolean carrierCertificate;

    @JsonProperty("B2CNationalSurcharge")
    private Boolean b2cNationalSurcharge;

    @JsonProperty("B2CInternationalSurcharge")
    private Boolean b2cInternationalSurcharge;

    @JsonProperty("SecurityFee")
    private Boolean securityFee;

    @JsonProperty("Insurance")
    private Boolean insurance;

    @JsonProperty("PortiPapiere")
    private Boolean portiPapiere;

    @JsonProperty("Custom1")
    private Boolean custom1;

    @JsonProperty("Custom2")
    private Boolean custom2;

    @JsonProperty("Custom3")
    private Boolean custom3;

    @JsonProperty("Custom4")
    private Boolean custom4;

    @JsonProperty("Custom5")
    private Boolean custom5;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("ErrorType")
    private Integer errorType;

    @JsonProperty("Price")
    private Double price;

    @JsonProperty("TotalPrice")
    private Double totalPrice;

    @JsonProperty("ExtraCostsTotalPrice")
    private Double extraCostsTotalPrice;

    @JsonProperty("Toll")
    private Double toll;

    @JsonProperty("TollPercent")
    private Double tollPercent;

    @JsonProperty("Diesel")
    private Double diesel;

    @JsonProperty("DieselPercent")
    private Double dieselPercent;

    @JsonProperty("IsConsolidated")
    private Boolean isConsolidated;

    @JsonProperty("IsConsolidatedSum")
    private Boolean isConsolidatedSum;

    @JsonProperty("hasPackagingType")
    private Boolean hasPackagingType;

    @JsonProperty("projectType")
    private Integer projectType;

    @JsonProperty("undefined")
    private String undefined;
}
