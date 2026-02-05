package com.jokati.invoice.mapper;

import com.jokati.invoice.dto.CountryRequestDTO;
import com.jokati.invoice.dto.CountryResponseDTO;
import com.jokati.invoice.model.Country;

public class CountryMapper {
	
	 private static final String EUROPE_FLAG_URL =
	            "https://upload.wikimedia.org/wikipedia/commons/b/b7/Flag_of_Europe.svg";

    public static Country toEntity(CountryRequestDTO dto) {
        return Country.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .locale(dto.getLocale())
                .build();
    }

    public static CountryResponseDTO toResponseDTO(Country country) {
        CountryResponseDTO dto = new CountryResponseDTO();
        dto.setId(country.getId());
        dto.setCode(country.getCode());
        dto.setName(country.getName());
        dto.setGetlocale(country.getLocale()); 

     // FIX: handle Europe (INT) separately
        String flagUrl;
        if ("INT".equalsIgnoreCase(country.getCode())) {
            flagUrl = EUROPE_FLAG_URL;
        } else {
            flagUrl = "https://flagcdn.com/w320/"
                    + country.getCode().toLowerCase()
                    + ".png";
        }

        dto.setFlagUrl(flagUrl);

        return dto;
    }
}
