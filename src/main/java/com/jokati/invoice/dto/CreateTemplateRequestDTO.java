package com.jokati.invoice.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTemplateRequestDTO {

	 	@NotBlank
	    private String name;
	    @NotBlank
	    private String subject;
	    @NotBlank
	    private String body;


}
