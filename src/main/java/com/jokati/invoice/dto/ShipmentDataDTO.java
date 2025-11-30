package com.jokati.invoice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ShipmentDataDTO {
    private String shipmentId;
    private LocalDate shipmentDate;
    private String zipCodeShipper;
    private String zipCodeConsignee;
    private String city;
    private String country;
    private Double length;
    private Double wide;
    private Double height;
    private Double loadingMeters;
    private Double cubicMeters;
    private Integer palletCount;
    private String packagingType;
    private Double effectiveWeight;
    private Boolean hasPackagingType;
    private Integer projectType;
    private Boolean expressNextDay;
    private Boolean shortWeekSurcharge;
    private Boolean bookingAvis;
    private Boolean express12;
    private Boolean express10;
    private Boolean express8;
    private Boolean fixDate;
    private Boolean eMailAvis;
    private Boolean phoneAvis;
    private Boolean dangerousGoodsSurcharge;
    private Boolean carrierCertificate;
    private Boolean b2cSurchargeNational;
    private Boolean b2cSurchargeInternational;
    private Boolean securityFee;
    private Boolean insurance;
    private Boolean porti;
}
