
package com.jokati.invoice.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.InvoiceListItemDTO;
import com.jokati.invoice.dto.InvoicePageResponseDTO;
import com.jokati.invoice.dto.InvoiceRequestDTO;
import com.jokati.invoice.dto.InvoiceResponseDTO;
import com.jokati.invoice.dto.ShipmentDTO;
import com.jokati.invoice.mapper.InvoiceMapper;
import com.jokati.invoice.model.Invoice;
import com.jokati.invoice.model.Shipment;
import com.jokati.invoice.repository.InvoiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository repository;
    private final InvoiceMapper mapper;
    private final MongoTemplate mongoTemplate;
    private final InvoiceReconciliationService invoiceReconciliation;

    @Transactional
    public InvoiceResponseDTO create(String pathCompanyId, InvoiceRequestDTO request) {
        // Validate path/body consistency
        if (request.getCompanyId() == null || !pathCompanyId.equals(request.getCompanyId())) {
            throw new IllegalArgumentException("companyId in path and body must match");
        }

        // Pre-check duplicate; prefer unique index at DB to throw DuplicateKeyException
        boolean exists = repository.existsByCompanyIdAndInvoiceNumber(pathCompanyId, request.getInvoiceNumber());
        if (exists) {
            // If you have a unique index, DB will throw DuplicateKeyException; we mirror as 409
            throw new DuplicateKeyException("Invoice already exists for companyId=" + pathCompanyId +
                    ", invoiceNumber=" + request.getInvoiceNumber());
        }

        Invoice entity = mapper.toEntity(request);
        entity.setCompanyId(pathCompanyId);

        // Persist
        Invoice saved = repository.save(entity);

        // Reconciliation pipeline
        Invoice reconciled = invoiceReconciliation.reconcileAndPersist(saved);

        return mapper.toResponse(reconciled);
    }

    /**
     * Fetches invoice details with paginated shipments.
     * Pagination is applied only on the embedded shipments list.
     * Also returns pagination metadata for frontend navigation.
     */
    public Object get(
            String companyId,
            String invoiceNumber,
            int page,
            int size
    ) {

        if (size <= 0) size = 10;
        if (size > 100) size = 100;

        Invoice entity = repository.findByCompanyIdAndInvoiceNumber(companyId, invoiceNumber)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invoice not found for companyId=" + companyId +
                        ", invoiceNumber=" + invoiceNumber
                ));

        // entity -> dto
        InvoiceResponseDTO dto = mapper.toResponse(entity);

        List<ShipmentDTO> allShipments = dto.getShipments();
        int totalElements = allShipments == null ? 0 : allShipments.size();

        int totalPages = (int) Math.ceil((double) totalElements / size);

        // page out-of-range guard
        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }

        int start = page * size;
        int end = Math.min(start + size, totalElements);

        List<ShipmentDTO> pagedShipments = List.of();
        if (start < totalElements) {
            pagedShipments = allShipments.subList(start, end);
        }

        dto.setShipments(pagedShipments);

        return Map.of(
                "invoice", dto,
                "pagination", Map.of(
                        "page", page,
                        "size", size,
                        "totalElements", totalElements,
                        "totalPages", totalPages,     
                        "hasNext", page < totalPages - 1,
                        "hasPrevious", page > 0
                )
        );
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

        List<Invoice> invoices = mongoTemplate.find(q, Invoice.class);
        return invoices.stream().map(mapper::toListItem).toList();
    }
    
    
    public InvoicePageResponseDTO listFilteredPaged(String companyId,
            String carrier,
            String invoiceNumber,
            String fromDate,
            String toDate,
            int page,
            int size) {

        if (size > 100) size = 100;

        Query q = new Query();
        q.addCriteria(Criteria.where("companyId").is(companyId));

      
        if (carrier != null && !carrier.isBlank()) {
            q.addCriteria(Criteria.where("carrier").is(carrier));
        }

       
        if (invoiceNumber != null && !invoiceNumber.isBlank()) {
            q.addCriteria(Criteria.where("invoiceNumber").is(invoiceNumber));
        }

        // Date filter (Mongo ISODate FIX)
        if (fromDate != null && toDate != null) {

            Instant fromInstant = LocalDate.parse(fromDate)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();

            Instant toInstant = LocalDate.parse(toDate)
                    .atTime(23, 59, 59)
                    .toInstant(ZoneOffset.UTC);

            q.addCriteria(Criteria.where("invoiceDate").gte(fromInstant).lte(toInstant));
        }

        // Sort
        q.with(Sort.by(Sort.Direction.DESC, "invoiceDate"));

        // Pagination
        q.skip((long) page * size);
        q.limit(size);

        List<Invoice> invoices = mongoTemplate.find(q, Invoice.class);

        long total = mongoTemplate.count(q.skip(0).limit(0), Invoice.class);

        List<InvoiceListItemDTO> dtoList = invoices.stream().map(mapper::toListItem).toList();

        return InvoicePageResponseDTO.builder()
                .data(dtoList)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages((int) Math.ceil((double) total / size))
                .build();
    }


    
    @Transactional
    public void delete(String companyId, String invoiceNumber) {
        Invoice entity = repository.findByCompanyIdAndInvoiceNumber(companyId, invoiceNumber)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found for companyId=" + companyId +
                        ", invoiceNumber=" + invoiceNumber));
        repository.delete(entity);
        log.info("Invoice deleted: companyId={}, invoiceNumber={}", companyId, invoiceNumber);
    }

    @Transactional
    public InvoiceResponseDTO replace(String companyId, String invoiceNumber, InvoiceRequestDTO request) {
        Invoice existing = repository.findByCompanyIdAndInvoiceNumber(companyId, invoiceNumber)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found for companyId=" + companyId +
                        ", invoiceNumber=" + invoiceNumber));

        if (request.getCompanyId() == null || !companyId.equals(request.getCompanyId())) {
            throw new IllegalArgumentException("companyId in path and body must match");
        }
        if (!invoiceNumber.equals(request.getInvoiceNumber())) {
            throw new IllegalArgumentException("invoiceNumber in path and body must match");
        }

        Invoice updated = mapper.toEntity(request);
        updated.setId(existing.getId());
        updated.setCompanyId(companyId);

        Invoice saved = repository.save(updated);
        return mapper.toResponse(saved);
    }

    @Transactional
    public InvoiceResponseDTO addShipment(String companyId, String invoiceNumber, ShipmentDTO dto) {
        Invoice entity = repository.findByCompanyIdAndInvoiceNumber(companyId, invoiceNumber)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found for companyId=" + companyId +
                        ", invoiceNumber=" + invoiceNumber));

        Shipment shipment = mapper.toShipment(dto);

        // Replace if exists (shipmentId unique per invoice)
        entity.getShipments().removeIf(s -> s.getShipmentId().equalsIgnoreCase(shipment.getShipmentId()));
        entity.getShipments().add(shipment);

        Invoice saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    private LocalDate parseDateFlexible(String input) {
        if (input == null || input.isBlank()) return null;
        try { return LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE); } catch (Exception ignored) {}
        try { return LocalDate.parse(input, DateTimeFormatter.ofPattern("dd/MM/yyyy")); } catch (Exception ignored) {}
        return null;
    }
}
