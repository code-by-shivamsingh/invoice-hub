package com.jokati.invoice.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendPlainEmailRequestDTO {
    @NotBlank @Email
    private String to;

    @NotBlank
    private String subject;

    @NotBlank
    private String html;
}
