
package com.jokati.invoice.service;

import com.jokati.invoice.dto.InvoiceListItemDTO;
import com.jokati.invoice.dto.InvoiceRequestDTO;
import com.jokati.invoice.dto.InvoiceResponseDTO;
import com.jokati.invoice.dto.ShipmentDTO;
import com.jokati.invoice.mapper.InvoiceMapper;
import com.jokati.invoice.model.Invoice;
import com.jokati.invoice.model.Shipment;
import com.jokati.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository repository;
    private final InvoiceMapper mapper;
    private final MongoTemplate mongoTemplate;

    public InvoiceResponseDTO create(String companyId, InvoiceRequestDTO request) throws Exception {
        if (request.getCompanyId() == null || !companyId.equals(request.getCompanyId())) {
            throw new Exception("companyId in path and body must match");
        }
        if (repository.existsByCompanyIdAndInvoiceNumber(companyId, request.getInvoiceNumber())) {
            throw new Exception("Invoice already exists for companyId=" + companyId +
                                ", invoiceNumber=" + request.getInvoiceNumber());
        }
        Invoice entity = mapper.toEntity(request);
        entity.setCompanyId(companyId);
        // status can be provided or computed in mapper during response
        Invoice saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    public InvoiceResponseDTO get(String companyId, String invoiceNumber) throws Exception {
        Invoice entity = repository.findByCompanyIdAndInvoiceNumber(companyId, invoiceNumber)
            .orElseThrow(() -> new Exception("Invoice not found for companyId=" + companyId +
                                             ", invoiceNumber=" + invoiceNumber));
        return mapper.toResponse(entity);
    }

    /** Filtered list for UI (Carrier, Invoice Number, From/To dates) */
    public List<InvoiceListItemDTO> listFiltered(String companyId,
                                                 String carrier,
                                                 String invoiceNumber,
                                                 String fromDate,
                                                 String toDate) {

        Query q = new Query();
        q.addCriteria(Criteria.where("companyId").is(companyId));

        if (carrier != null && !carrier.isBlank() && !"All".equalsIgnoreCase(carrier)) {
            q.addCriteria(Criteria.where("seller.companyName").is(carrier));
        }

        if (invoiceNumber != null && !invoiceNumber.isBlank()) {
            q.addCriteria(Criteria.where("invoiceNumber").is(invoiceNumber));
        }

        LocalDate from = parseDateFlexible(fromDate);
        LocalDate to   = parseDateFlexible(toDate);

        if (from != null && to != null) {
            q.addCriteria(Criteria.where("invoiceDate").gte(from).lte(to));
        } else if (from != null) {
            q.addCriteria(Criteria.where("invoiceDate").gte(from));
        } else if (to != null) {
            q.addCriteria(Criteria.where("invoiceDate").lte(to));
        }

        // Optionally sort by date desc
        // q.with(Sort.by(Sort.Direction.DESC, "invoiceDate"));

        List<Invoice> invoices = mongoTemplate.find(q, Invoice.class);
        return invoices.stream().map(mapper::toListItem).toList();
    }

    public void delete(String companyId, String invoiceNumber) throws Exception {
        Invoice entity = repository.findByCompanyIdAndInvoiceNumber(companyId, invoiceNumber)
            .orElseThrow(() -> new Exception("Invoice not found for companyId=" + companyId +
                                             ", invoiceNumber=" + invoiceNumber));
        repository.delete(entity);
    }

    public InvoiceResponseDTO replace(String companyId, String invoiceNumber, InvoiceRequestDTO request) throws Exception {
        Invoice existing = repository.findByCompanyIdAndInvoiceNumber(companyId, invoiceNumber)
            .orElseThrow(() -> new Exception("Invoice not found for companyId=" + companyId +
                                             ", invoiceNumber=" + invoiceNumber));
        if (request.getCompanyId() == null || !companyId.equals(request.getCompanyId())) {
            throw new Exception("companyId in path and body must match");
        }
        if (!invoiceNumber.equals(request.getInvoiceNumber())) {
            throw new Exception("invoiceNumber in path and body must match");
        }

        Invoice updated = mapper.toEntity(request);
        updated.setId(existing.getId());
        updated.setCompanyId(companyId);
        Invoice saved = repository.save(updated);
        return mapper.toResponse(saved);
    }

    public InvoiceResponseDTO addShipment(String companyId, String invoiceNumber, ShipmentDTO dto) throws Exception {
        Invoice entity = repository.findByCompanyIdAndInvoiceNumber(companyId, invoiceNumber)
            .orElseThrow(() -> new Exception("Invoice not found for companyId=" + companyId +
                                             ", invoiceNumber=" + invoiceNumber));
        Shipment shipment = mapper.toShipment(dto);
        entity.getShipments().removeIf(s -> s.getShipmentId().equalsIgnoreCase(shipment.getShipmentId()));
        entity.getShipments().add(shipment);
        Invoice saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    private LocalDate parseDateFlexible(String input) {
        if (input == null || input.isBlank()) return null;
        // Support both "yyyy-MM-dd" and "dd/MM/yyyy" (as per UI)
        try { return LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE); } catch (Exception ignored) {}
        try { return LocalDate.parse(input, DateTimeFormatter.ofPattern("dd/MM/yyyy")); } catch (Exception ignored) {}
        return null;
    }
}
