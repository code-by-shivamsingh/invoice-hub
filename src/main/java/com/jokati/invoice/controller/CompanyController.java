package com.jokati.invoice.controller;

import com.jokati.invoice.dto.CompanyRequestDTO;
import com.jokati.invoice.dto.CompanyResponseDTO;
import com.jokati.invoice.service.CompanyService;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.converters.models.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.print.Pageable;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService service;
    // ✅ CREATE
    @PostMapping
    public CompanyResponseDTO create(@RequestBody CompanyRequestDTO dto) {
       
        return service.createCompany(dto);
    }

    @GetMapping
    public Page<CompanyResponseDTO> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.getAll(page, size);
    }


    // ✅ GET BY ID
    @GetMapping("/{companyId}")
    public ResponseEntity<?> getById(@PathVariable String companyId) {

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", 200,
                "message", "Company fetched successfully",
                "data", service.getByCompanyId(companyId)
        ));
    }

    // ✅ UPDATE
    @PutMapping("/{companyId}")
    public ResponseEntity<?> update(
            @PathVariable String companyId,
            @RequestBody CompanyRequestDTO dto) {

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", 200,
                "message", "Company updated successfully",
                "data", service.update(companyId, dto)
        ));
    }

    // ✅ DELETE
    @DeleteMapping("/{companyId}")
    public ResponseEntity<?> delete(@PathVariable String companyId) {

        service.delete(companyId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", 200,
                "message", "Company deleted successfully"
        ));
    }
}