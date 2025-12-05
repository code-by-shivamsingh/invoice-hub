
package com.jokati.invoice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewUserRequestDTO {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    // Optional fields you may be sending
    private String company;
    private String firstName;
    private String lastName;
}
