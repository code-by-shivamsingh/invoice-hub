package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanyResponseDTO {

    private String id;

    private String email;

    @JsonProperty("company")  
    private String companyName;

    private String firstName;
    private String lastName;

    private LocalDate licenseStart;
    private LocalDate licenseEnd;

    private String role;

    private List<String> modules; 

    private Boolean mayCreateAdditionalUsers;
    private Integer maxAdditionalUsers;

    private String password;
    private String repeatPassword;

    private Integer totalUsers;
    private Integer activeUsers;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}