package com.jokati.invoice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.ManualDecisionRequest;
import com.jokati.invoice.model.StatusInfo;
import com.jokati.invoice.service.ManualInvoiceDecisionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invoices")
public class InvoiceDecisionController {

    private final ManualInvoiceDecisionService decisionService;

    @PostMapping("/decision")
    public ResponseEntity<ApiResponse> decision(
            @Valid @RequestBody ManualDecisionRequest request) {

        decisionService.decideByInvoiceNumber(
                request.getCompanyId(),
                request.getInvoiceNumber(),
                decisionService.mapFinalStatus(request.getStatus()),
                "SYSTEM_USER",   
                "FINANCE",
                request.getRemark()
        );

        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setCode(200);
        response.setMessage("Invoice decision updated successfully");
        response.setData(null);

        return ResponseEntity.ok(response);
    }


}
