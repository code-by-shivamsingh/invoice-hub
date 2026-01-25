package com.jokati.invoice.dto;

import lombok.Data;

@Data
public class CountryResponseDTO {
    private String id;
    private String code;
    private String name;
    private String getlocale;
    private String flagUrl;

}
