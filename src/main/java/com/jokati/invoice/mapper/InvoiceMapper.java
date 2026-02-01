
package com.jokati.invoice.mapper;

import java.time.LocalDate;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.web.util.HtmlUtils;

import com.jokati.invoice.dto.ChargesDTO;
import com.jokati.invoice.dto.InvoiceListItemDTO;
import com.jokati.invoice.dto.InvoiceRequestDTO;
import com.jokati.invoice.dto.InvoiceResponseDTO;
import com.jokati.invoice.dto.ShipmentDTO;
import com.jokati.invoice.dto.TotalsDTO;
import com.jokati.invoice.model.Charges;
import com.jokati.invoice.model.Invoice;
import com.jokati.invoice.model.Shipment;
import com.jokati.invoice.model.StatusInfo;
import com.jokati.invoice.model.Totals;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    // --- Helpers ---
    default LocalDate toLocalDate(String isoDate) { return isoDate == null ? null : LocalDate.parse(isoDate); }
    default String toIsoDate(LocalDate date) { return date == null ? null : date.toString(); }
    default String unescapeHtml(String s) { return s == null ? null : HtmlUtils.htmlUnescape(s); }
    default StatusInfo prettyStatusInfo(StatusInfo status) {if (status == null) return null;

        String[] parts = status.getLabel().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();

        for (String p : parts) {sb.append(Character.toUpperCase(p.charAt(0)))
              .append(p.substring(1))
              .append(" ");
        }

        return StatusInfo.builder()
                .label(sb.toString().trim())   // Manually Accepted
                .color(status.getColor())
                .build();
    }


    // --- Totals ---
    Totals toTotals(TotalsDTO dto);
    TotalsDTO toTotalsDTO(Totals totals);

    // --- Charges ---
    Charges toCharges(ChargesDTO dto);
    ChargesDTO toChargesDTO(Charges charges);

    // --- Shipment ---
    @Mappings({
        @Mapping(target = "charges",    source = "charges"),
        @Mapping(target = "orderTotal", source = "orderTotal"),
        @Mapping(target = "difference", source = "difference"),
        @Mapping(target = "status",     ignore = true) // status computed later or mapped elsewhere
    })
    Shipment toShipment(ShipmentDTO dto);

    @Mappings({
        @Mapping(target = "charges",    source = "charges"),
        @Mapping(target = "orderTotal", source = "orderTotal"),
        @Mapping(target = "difference", source = "difference"),
        @Mapping(target = "status",     source = "status")
    })
    ShipmentDTO toShipmentDTO(Shipment shipment);

    // --- Invoice: Request -> Entity ---
    // Ignore fields not present on request or computed later (dueDate, status, invoiceDifference, extractionScope)
    @Mappings({
        @Mapping(target = "companyId",         source = "companyId"),
        @Mapping(target = "carrier",           source = "carrier"),
        @Mapping(target = "invoiceNumber",     source = "invoiceNumber"),
        @Mapping(target = "invoiceDate",       source = "invoiceDate"),
        @Mapping(target = "currency",          source = "currency"),
        @Mapping(target = "totals",            source = "totals"),
        @Mapping(target = "shipments",         source = "shipments"),
        @Mapping(target = "orderTotal",        source = "orderTotal"),
        @Mapping(target = "emailStatus",       source = "emailStatus"),
        @Mapping(target = "systemStatus",      ignore = true),
        @Mapping(target = "finalStatus",       ignore = true),
        @Mapping(target = "invoiceDifference", ignore = true),
        // These targets do not exist on the model now intentionally: seller, billTo, shipmentSummary, dueDate
        // If you reintroduce them, map explicitly or ignore as needed.
    })
    Invoice toEntity(InvoiceRequestDTO dto);

    // --- Invoice: Entity -> Response ---
    @Mappings({
        @Mapping(target = "companyId",        source = "companyId"),
        @Mapping(target = "invoiceNumber",    source = "invoiceNumber"),
        @Mapping(target = "invoiceDate",      expression = "java(toIsoDate(entity.getInvoiceDate()))"),
        @Mapping(target = "dueDate",          expression = "java(toIsoDate(null))"), // no dueDate in model now
        @Mapping(target = "currency",         source = "currency"),
        @Mapping(target = "totals",           source = "totals"),
        @Mapping(target = "shipments",        source = "shipments"),
        @Mapping(target = "orderTotal",       source = "orderTotal"),
        @Mapping(target = "carrier",          source = "carrier"),
        @Mapping(target = "invoiceTotal",     expression = "java(entity.getTotals() != null ? entity.getTotals().getGrossAmount() : null)"),
        @Mapping(target = "difference",       source = "invoiceDifference"),
        @Mapping(target = "status",           expression = "java(" +"entity.getFinalStatus() != null " +"? prettyStatusInfo(entity.getFinalStatus()) " +": prettyStatusInfo(entity.getSystemStatus())" +")"),
        @Mapping(target = "emailStatus",      source = "emailStatus"),
        @Mapping(target = "createdAt",        source = "createdAt"),
        @Mapping(target = "updatedAt",        source = "updatedAt")
    })
    InvoiceResponseDTO toResponse(Invoice entity);

    // --- Invoice: Entity -> List Item ---
    @Mappings({
        @Mapping(target = "invoiceNumber", source = "invoiceNumber"),
        @Mapping(target = "invoiceDate",   expression = "java(toIsoDate(entity.getInvoiceDate()))"),
        @Mapping(target = "carrier",       source = "carrier"),
        @Mapping(target = "orderTotal",    source = "orderTotal"),
        @Mapping(target = "invoiceTotal",  expression = "java(entity.getTotals() != null ? entity.getTotals().getGrossAmount() : null)"),
        @Mapping(target = "difference",    source = "invoiceDifference"),
        @Mapping(target = "status",        expression = "java(" + "entity.getFinalStatus() != null " +"? prettyStatusInfo(entity.getFinalStatus()) " +": prettyStatusInfo(entity.getSystemStatus())" +")"),
        @Mapping(target = "emailStatus",   source = "emailStatus"),
        @Mapping(target = "shipments",     ignore = true)  
    })
    InvoiceListItemDTO toListItem(Invoice entity);

    // --- List mapping support (for shipments) ---
    List<ShipmentDTO> toShipmentDTOList(List<Shipment> shipments);
}