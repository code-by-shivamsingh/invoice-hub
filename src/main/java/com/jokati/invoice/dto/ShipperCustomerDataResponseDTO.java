package com.jokati.invoice.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;

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
