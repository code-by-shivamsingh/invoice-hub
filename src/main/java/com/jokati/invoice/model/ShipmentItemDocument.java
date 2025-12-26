
package com.jokati.invoice.model;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentItemDocument {

    // Mirror both "Id" and "id" in Mongo
    @Field("Id")
    private String idUpper;

    @Field("id")
    private String idLower;

    @Field("ShipmentId")
    private String shipmentId;       // keep newline prefix if sent

    @Field("ShipmentDate")
    private String shipmentDate;     // keep "yyyy-M-d" or any string sent

    @Field("ZipCodeShipper")
    private String zipCodeShipper;

    @Field("ZipCodeConsignee")
    private String zipCodeConsignee;

    @Field("City")
    private String city;

    @Field("Country")
    private String country;

    @Field("Length")
    private Double length;

    @Field("Wide")
    private Double wide;

    @Field("Height")
    private Double height;

    @Field("LoadingMeters")
    private Double loadingMeters;

    @Field("CubicMeters")
    private Double cubicMeters;

    @Field("PalletCount")
    private Integer palletCount;

    @Field("PackagingType")
    private String packagingType;

    @Field("EffectiveWeight")
    private Double effectiveWeight;

    @Field("ChargeableWeight")
    private Double chargeableWeight;

    @Field("MinimumWeight")
    private Double minimumWeight;

    @Field("WeightByCubicMeters")
    private Double weightByCubicMeters;

    @Field("WeightByLoadingMeters")
    private Double weightByLoadingMeters;

    @Field("Stackable")
    private Boolean stackable;

    @Field("StackFactor")
    private Integer stackFactor;

    @Field("StackId")
    private String stackId;

    @Field("StackFootprintLoadingMeters")
    private Double stackFootprintLoadingMeters;

    @Field("ExpressNextDay")
    private Boolean expressNextDay;

    @Field("Express12")
    private Boolean express12;

    @Field("Express10")
    private Boolean express10;

    @Field("Express8")
    private Boolean express8;

    @Field("Fixtermin")
    private Boolean fixtermin;

    @Field("EmailAvis")
    private Boolean emailAvis;

    @Field("PhoneAvis")
    private Boolean phoneAvis;

    @Field("BookingInAvis")
    private Boolean bookingInAvis;

    @Field("DangerousGoodsSurcharge")
    private Boolean dangerousGoodsSurcharge;

    @Field("LongGoodsSurcharge")
    private Boolean longGoodsSurcharge;

    @Field("ShortWeekSurcharge")
    private Boolean shortWeekSurcharge;

    @Field("PalletExchange")
    private Boolean palletExchange;

    @Field("PalletBoxExchange")
    private Boolean palletBoxExchange;

    @Field("CarrierCertificate")
    private Boolean carrierCertificate;

    @Field("B2CNationalSurcharge")
    private Boolean b2cNationalSurcharge;

    @Field("B2CInternationalSurcharge")
    private Boolean b2cInternationalSurcharge;

    @Field("SecurityFee")
    private Boolean securityFee;

    @Field("Insurance")
    private Boolean insurance;

    @Field("PortiPapiere")
    private Boolean portiPapiere;

    @Field("Custom1")
    private Boolean custom1;

    @Field("Custom2")
    private Boolean custom2;

    @Field("Custom3")
    private Boolean custom3;

    @Field("Custom4")
    private Boolean custom4;

    @Field("Custom5")
    private Boolean custom5;

    @Field("Message")
    private String message;

    @Field("ErrorType")
    private Integer errorType;

    @Field("Price")
    private Double price;

    @Field("TotalPrice")
    private Double totalPrice;

    @Field("ExtraCostsTotalPrice")
    private Double extraCostsTotalPrice;

    @Field("Toll")
    private Double toll;

    @Field("TollPercent")
    private Double tollPercent;

    @Field("Diesel")
    private Double diesel;

    @Field("DieselPercent")
    private Double dieselPercent;

    @Field("IsConsolidated")
    private Boolean isConsolidated;

    @Field("IsConsolidatedSum")
    private Boolean isConsolidatedSum;

    @Field("hasPackagingType")
    private Boolean hasPackagingType;

    @Field("projectType")
    private Integer projectType;

    @Field("undefined")
    private String undefined;
    

	@Field("HasFP")
	    private Boolean hasFP;
	
	@Field("FPPalletCount")
	    private Integer fpPalletCount;

}

