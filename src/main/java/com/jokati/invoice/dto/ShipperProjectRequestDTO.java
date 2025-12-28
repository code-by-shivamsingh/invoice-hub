
package com.jokati.invoice.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for creating a project. Mirrors Node validation on userId and name.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperProjectRequestDTO {

    @NotBlank(message = "userId is required")
    private String userId;  // Customer No is userId
    
    private String companyId; 

    @NotBlank(message = "name is required")
    private String name;  // company name 
    
    @NotBlank(message = "Street is required")
    private String street;
    
    @NotBlank(message = "Street No is required")
    private String streetNo;
    
    @NotBlank(message = "ZipCode No is required")
    private String zipCode;
    
    
    @NotBlank(message = "City No is required")
    private String city;
    
    @NotBlank(message = "Country No is required")
    private String country;
    
    @NotBlank(message = "ContactName No is required")
    private String contactName;
    
    // @NotBlank(message = "Phone No is required")
    private Double phoneNo;
    
    @NotBlank(message = "Email Id is required")
    private String email;
    
    private Boolean active;
    
    

    /** Additional dynamic fields, equivalent to Mongoose { strict: false } */
    private Map<String, Object> extra;
}
