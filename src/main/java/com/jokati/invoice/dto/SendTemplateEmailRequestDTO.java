package com.jokati.invoice.dto;



import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendTemplateEmailRequestDTO {

	@NotBlank @Email
    private String to;

    private String templateId;     // or use templateName
    private String templateName;

    private Map<String, Object> model = new HashMap<>();


}
