
package com.jokati.invoice.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperProjectResponseDTO {
    private String id;                     // hex string of ObjectId
    private String userId;
    private String name;
    private String street;
    private String streetNo;
    private String zipCode;
    private String city;
    private String country;
    private String contactName;
    private Double phoneNo;
    private String email;
    private Map<String, Object> extra;     // dynamic content
    private Instant createdAt;
    private Instant updatedAt;
}
