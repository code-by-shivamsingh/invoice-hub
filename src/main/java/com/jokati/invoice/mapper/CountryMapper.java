package com.jokati.invoice.mapper;

import com.jokati.invoice.dto.CountryRequestDTO;
import com.jokati.invoice.dto.CountryResponseDTO;
import com.jokati.invoice.model.Country;

public class CountryMapper {

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

        // Flag URL generate
        dto.setFlagUrl("https://flagcdn.com/w320/" 
                + country.getCode().toLowerCase() + ".png");

        return dto;
    }

}
