package com.jokati.invoice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualDecisionRequest {

    private String companyId;
    private String invoiceNumber;

    // MANUALLY_ACCEPTED / MANUALLY_REJECTED
    private String status;

    private String remark;
}
