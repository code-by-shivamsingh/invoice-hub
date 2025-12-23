
package com.jokati.invoice.mapper;

import com.jokati.invoice.dto.ChargesDTO;
import com.jokati.invoice.dto.InvoiceListItemDTO;
import com.jokati.invoice.dto.InvoiceRequestDTO;
import com.jokati.invoice.dto.InvoiceResponseDTO;
import com.jokati.invoice.dto.PartyDTO;
import com.jokati.invoice.dto.ShipmentDTO;
import com.jokati.invoice.dto.ShipmentSummaryDTO;
import com.jokati.invoice.dto.TotalsDTO;

import com.jokati.invoice.model.Charges;
import com.jokati.invoice.model.Invoice;
import com.jokati.invoice.model.Party;
import com.jokati.invoice.model.Shipment;
import com.jokati.invoice.model.ShipmentSummary;
import com.jokati.invoice.model.Totals;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    // --- Helpers ---
    default LocalDate toLocalDate(String isoDate) { return isoDate == null ? null : LocalDate.parse(isoDate); }
    default String toIsoDate(LocalDate date) { return date == null ? null : date.toString(); }
    default String unescapeHtml(String s) { return s == null ? null : HtmlUtils.htmlUnescape(s); }

    // --- Party ---
    @Mappings({
        @Mapping(target = "companyName", expression = "java(unescapeHtml(dto.getCompanyName()))")
    })
    Party toParty(PartyDTO dto);

    @Mappings({
        @Mapping(target = "companyName", expression = "java(unescapeHtml(party.getCompanyName()))")
    })
    PartyDTO toPartyDTO(Party party);

    // --- Totals ---
    Totals toTotals(TotalsDTO dto);
    TotalsDTO toTotalsDTO(Totals totals);

    // --- ShipmentSummary ---
    ShipmentSummary toShipmentSummary(ShipmentSummaryDTO dto);
    ShipmentSummaryDTO toShipmentSummaryDTO(ShipmentSummary summary);

    // --- Charges ---
    Charges toCharges(ChargesDTO dto);
    ChargesDTO toChargesDTO(Charges charges);

    // --- Shipment: DTO → Entity (compute difference/status on entity) ---
    @Mappings({
        @Mapping(target = "pickupDate", expression = "java(toLocalDate(dto.getPickupDate()))"),
        @Mapping(target = "charges",    source = "charges"),
        @Mapping(target = "orderTotal", source = "orderTotal"),
        // Compute difference/status at entity level for consistency
        @Mapping(target = "difference", expression = "java(calcShipmentDifference(dto))"),
        @Mapping(target = "status",     expression = "java(calcShipmentStatus(dto))")
    })
    Shipment toShipment(ShipmentDTO dto);

    // --- Shipment: Entity → DTO (preserve computed fields) ---
    @Mappings({
        @Mapping(target = "pickupDate", expression = "java(toIsoDate(shipment.getPickupDate()))"),
        @Mapping(target = "charges",    source = "charges"),
        @Mapping(target = "orderTotal", source = "orderTotal"),
        @Mapping(target = "difference", source = "difference"),
        @Mapping(target = "status",     source = "status")
    })
    ShipmentDTO toShipmentDTO(Shipment shipment);

    // --- Invoice entity ← request DTO ---
    @Mappings({
        @Mapping(target = "invoiceDate",     expression = "java(toLocalDate(dto.getInvoiceDate()))"),
        @Mapping(target = "dueDate",         expression = "java(toLocalDate(dto.getDueDate()))"),
        @Mapping(target = "seller",          source = "seller"),
        @Mapping(target = "billTo",          source = "billTo"),
        @Mapping(target = "totals",          source = "totals"),
        @Mapping(target = "shipmentSummary", source = "shipmentSummary"),
        @Mapping(target = "shipments",       source = "shipments"),
        @Mapping(target = "orderTotal",      source = "orderTotal"),
        @Mapping(target = "status",          source = "status")
    })
    Invoice toEntity(InvoiceRequestDTO dto);

    // --- Invoice entity → response DTO ---
    @Mappings({
        @Mapping(target = "invoiceDate",     expression = "java(toIsoDate(entity.getInvoiceDate()))"),
        @Mapping(target = "dueDate",         expression = "java(toIsoDate(entity.getDueDate()))"),
        @Mapping(target = "seller",          source = "seller"),
        @Mapping(target = "billTo",          source = "billTo"),
        @Mapping(target = "totals",          source = "totals"),
        @Mapping(target = "shipmentSummary", source = "shipmentSummary"),
        @Mapping(target = "shipments",       source = "shipments"),
        @Mapping(target = "orderTotal",      source = "orderTotal"),
        @Mapping(target = "carrier",
                 expression = "java(entity.getSeller() != null ? unescapeHtml(entity.getSeller().getCompanyName()) : null)"),
        @Mapping(target = "invoiceTotal",
                 expression = "java(entity.getTotals() != null ? entity.getTotals().getGrossAmount() : null)"),
        @Mapping(target = "difference",      expression = "java(calcDifferenceNullable(entity))"),
        @Mapping(target = "status",          expression = "java(calcStatus(entity))")
    })
    InvoiceResponseDTO toResponse(Invoice entity);

    // --- Filter list item (getAll/filtered) includes shipments too ---
    @Mappings({
        @Mapping(target = "invoiceNumber", source = "invoiceNumber"),
        @Mapping(target = "invoiceDate",   expression = "java(toIsoDate(entity.getInvoiceDate()))"),
        @Mapping(target = "carrier",
                 expression = "java(entity.getSeller() != null ? unescapeHtml(entity.getSeller().getCompanyName()) : null)"),
        @Mapping(target = "orderTotal",    source = "orderTotal"),
        @Mapping(target = "invoiceTotal",
                 expression = "java(entity.getTotals() != null ? entity.getTotals().getGrossAmount() : null)"),
        @Mapping(target = "difference",    expression = "java(calcDifferenceNullable(entity))"),
        @Mapping(target = "status",        expression = "java(calcStatus(entity))"),
        @Mapping(target = "shipments",     source = "shipments")
    })
    InvoiceListItemDTO toListItem(Invoice entity);

    // --- PATCH (if re-enabled later) ---
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    void updateEntityFromPatch(InvoicePatchDTO patch, @MappingTarget Invoice entity);

    // --- Invoice-level Status / Difference logic ---
    /** Return null difference when orderTotal or invoiceTotal missing, to avoid misleading "0". */
    default BigDecimal calcDifferenceNullable(Invoice entity) {
        BigDecimal order = entity.getOrderTotal();
        BigDecimal inv   = entity.getTotals() != null ? entity.getTotals().getGrossAmount() : null;
        if (order == null || inv == null) return null;
        return order.subtract(inv);
    }

    /** "Correct billing" only when orderTotal exists and difference == 0, else "Incorrect billing". */
    default String calcStatus(Invoice entity) {
        BigDecimal diff = calcDifferenceNullable(entity);
        if (entity.getStatus() != null && !entity.getStatus().isBlank()) {
            return entity.getStatus();
        }
        return (diff != null && diff.compareTo(BigDecimal.ZERO) == 0) ? "Correct billing" : "Incorrect billing";
    }

    // --- Shipment-level Difference / Status logic (used in DTO → entity mapping) ---
    default BigDecimal calcShipmentDifference(ShipmentDTO dto) {
        BigDecimal order = dto.getOrderTotal();
        BigDecimal inv   = dto.getNetAmountEur();
        if (order == null || inv == null) return null;
        return order.subtract(inv);
    }

    default String calcShipmentStatus(ShipmentDTO dto) {
        BigDecimal diff = calcShipmentDifference(dto);
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            return dto.getStatus();
        }
        return (diff != null && diff.compareTo(BigDecimal.ZERO) == 0) ? "Correct billing" : "Incorrect billing";
    }

    // --- List mapping support (for shipments) ---
    List<ShipmentDTO> toShipmentDTOList(List<Shipment> shipments);
}
