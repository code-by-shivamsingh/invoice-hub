package com.jokati.invoice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePageResponseDTO {

    private List<InvoiceListItemDTO> data;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
