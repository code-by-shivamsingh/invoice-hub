
package com.jokati.invoice.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Party {
    @NotBlank private String companyName;
    @NotBlank private String address;
    @NotBlank private String vatId;
    @NotBlank private String country;
}
