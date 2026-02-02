package com.jokati.invoice.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.jokati.invoice.model.InvoiceStatusHistory;
import com.jokati.invoice.repository.InvoiceStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceStatusHistoryService {

    private final InvoiceStatusHistoryRepository repo;

   
    public void saveSystem(String invoiceId, String newStatus) {
        InvoiceStatusHistory h = new InvoiceStatusHistory();
        h.setInvoiceId(invoiceId);
        h.setPreviousStatus(null);
        h.setNewStatus(newStatus);
        h.setUpdatedBy("SYSTEM");
        h.setUpdatedByRole("SYSTEM");
        h.setUpdatedAt(Instant.now());
        repo.save(h);
    }

    
    public void saveManual(String invoiceId,
                           String previousStatus,
                           String newStatus,
                           String userId,
                           String role,
                           String remark) {

        InvoiceStatusHistory h = new InvoiceStatusHistory();
        h.setInvoiceId(invoiceId);
        h.setPreviousStatus(previousStatus);
        h.setNewStatus(newStatus);
        h.setUpdatedBy(userId);
        h.setUpdatedByRole(role);
        h.setRemark(remark);
        h.setUpdatedAt(Instant.now());
        repo.save(h);
    }
}