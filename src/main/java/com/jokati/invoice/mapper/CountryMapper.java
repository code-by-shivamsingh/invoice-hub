package com.jokati.invoice.mapper;

import com.jokati.invoice.dto.CountryRequestDTO;
import com.jokati.invoice.dto.CountryResponseDTO;
import com.jokati.invoice.model.Country;

public class CountryMapper {

    public static Country toEntity(CountryRequestDTO dto) {
        return Country.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .build();
    }

    public static CountryResponseDTO toResponseDTO(Country country) {
        CountryResponseDTO dto = new CountryResponseDTO();
        dto.setId(country.getId());
        dto.setCode(country.getCode());
        dto.setName(country.getName());
        return dto;
    }
}
