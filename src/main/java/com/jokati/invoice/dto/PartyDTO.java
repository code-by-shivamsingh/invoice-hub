
package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartyDTO {
    @JsonProperty("company_name") @NotBlank private String companyName;
    @JsonProperty("address")      @NotBlank private String address;
    @JsonProperty("vat_id")       @NotBlank private String vatId;
    @JsonProperty("country")      @NotBlank private String country;
}
