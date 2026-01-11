package com.jokati.invoice.dto;



import lombok.Data;

/** All fields optional for partial update. */
@Data
public class UpdateTemplateRequestDTO {

	private String name;
    private String subject;
    private String body;


}
