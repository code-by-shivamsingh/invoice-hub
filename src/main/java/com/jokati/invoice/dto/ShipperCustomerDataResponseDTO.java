package com.jokati.invoice.dto;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperCustomerDataResponseDTO {
    private String id;
    private Map<String, Object> data;
    private Instant createdAt;
    private Instant updatedAt;
}
