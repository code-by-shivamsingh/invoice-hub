package com.jokati.invoice.dto;

import java.util.List;
import lombok.*;

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
