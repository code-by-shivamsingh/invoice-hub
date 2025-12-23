
package com.jokati.invoice.repository;

import com.jokati.invoice.model.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Optional<Invoice> findByCompanyIdAndInvoiceNumber(String companyId, String invoiceNumber);
    boolean existsByCompanyIdAndInvoiceNumber(String companyId, String invoiceNumber);
    List<Invoice> findByCompanyId(String companyId);
    void deleteByCompanyIdAndInvoiceNumber(String companyId, String invoiceNumber);
}
