
package com.jokati.invoice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.jokati.invoice.dto.ToleranceDTO;
import com.jokati.invoice.dto.ToleranceLimitsPatchRequestDTO;
import com.jokati.invoice.dto.ToleranceLimitsRequestDTO;
import com.jokati.invoice.dto.ToleranceLimitsResponseDTO;
import com.jokati.invoice.model.Tolerance;
import com.jokati.invoice.model.ToleranceLimits;

@Mapper(componentModel = "spring")
public interface ToleranceMapper {

    ToleranceLimits toEntity(ToleranceLimitsRequestDTO request);
    ToleranceLimitsResponseDTO toResponse(ToleranceLimits entity);

    Tolerance toEntity(ToleranceDTO dto);
    ToleranceDTO toDto(Tolerance entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromPatch(ToleranceLimitsPatchRequestDTO patch, @MappingTarget ToleranceLimits entity);
}
