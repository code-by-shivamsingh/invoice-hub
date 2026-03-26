package com.jokati.invoice.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CompanyRequestDTO {

   
    private String email;
    private String company;        
    private String firstName;
    private String lastName;
    private String role;

    private LocalDate licenseStart;

    private List<String> modules;  

    private Boolean mayCreateAdditionalUsers;
    private Integer maxAdditionalUsers;

    private String password;
    private String repeatPassword;
}