package com.jokati.invoice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "shipment-data")
public class ShipmentData {

    @Id
    private String id;

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

    private String projectId;
    private String createdAt;
}
