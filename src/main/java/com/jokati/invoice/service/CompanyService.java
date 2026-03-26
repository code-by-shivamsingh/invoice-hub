package com.jokati.invoice.service;

import com.jokati.invoice.dto.CompanyRequestDTO;
import com.jokati.invoice.dto.CompanyResponseDTO;
import com.jokati.invoice.dto.UserResponseDTO;
import com.jokati.invoice.model.Company;
import com.jokati.invoice.model.User;
import com.jokati.invoice.repository.CompanyRepository;
import com.jokati.invoice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepo;
    private final UserRepository userRepo;

    // ✅ CREATE
    public CompanyResponseDTO createCompany(CompanyRequestDTO dto) {

        Company company = new Company();

        
        company.setCompanyId(dto.getCompany());   
        company.setCompanyName(dto.getCompany()); 

        company.setLicenseStart(dto.getLicenseStart());

        // ✅ NULL SAFE
        company.setLicenseEnd(
                dto.getLicenseStart() != null
                        ? dto.getLicenseStart().plusYears(1)
                        : null
        );

        company.setModules(dto.getModules()); 
        company.setMayCreateAdditionalUsers(dto.getMayCreateAdditionalUsers());
        company.setMaxAdditionalUsers(dto.getMaxAdditionalUsers());

        company.setTotalUsers(1);
        company.setActiveUsers(1);

        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());

        companyRepo.save(company);

        return mapToResponse(company, dto, null);
    }

    // ✅ GET ALL
    public Page<CompanyResponseDTO> getAll(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return companyRepo.findAll(pageable)
                .map(c -> mapToResponse(c, null, null));
    }

    // ✅ GET BY ID (Company + Users)
    public CompanyResponseDTO getByCompanyId(String companyId) {

        Company company = companyRepo.findByCompanyId(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        List<UserResponseDTO> users = userRepo.findByCompanyId(companyId)
                .stream()
                .map(this::mapUser)
                .toList();

        return mapToResponse(company, null, users);
    }

    // ✅ UPDATE
    public CompanyResponseDTO update(String companyId, CompanyRequestDTO dto) {

        Company company = companyRepo.findByCompanyId(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // 🔥 UPDATE mapping bhi same hona chahiye
        company.setCompanyName(dto.getCompany());
        company.setModules(dto.getModules());
        company.setUpdatedAt(LocalDateTime.now());

        companyRepo.save(company);

        return mapToResponse(company, dto, null);
    }

    // ✅ DELETE
    public void delete(String companyId) {

        Company company = companyRepo.findByCompanyId(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        companyRepo.delete(company);
    }

    // 🔁 COMPANY MAPPER
    private CompanyResponseDTO mapToResponse(Company c, CompanyRequestDTO dto, List<UserResponseDTO> users) {

        return CompanyResponseDTO.builder()
                // 🔥 Mongo ID
                .id(c.getId() != null ? c.getId().toString() : null)

               
                .email(dto != null ? dto.getEmail() : null)
                .firstName(dto != null ? dto.getFirstName() : null)
                .lastName(dto != null ? dto.getLastName() : null)
                .role(dto != null ? dto.getRole() : null)
                .companyName(c.getCompanyName())
                .licenseStart(c.getLicenseStart())
                .licenseEnd(c.getLicenseEnd())
                .modules(c.getModules())
                .mayCreateAdditionalUsers(c.getMayCreateAdditionalUsers()) 
                .maxAdditionalUsers(c.getMaxAdditionalUsers())
                .totalUsers(c.getTotalUsers())
                .activeUsers(c.getActiveUsers())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .password(dto != null ? dto.getPassword() : null)
                .repeatPassword(dto != null ? dto.getRepeatPassword() : null)

                .build();
    }

    // 🔁 USER MAPPER
    private UserResponseDTO mapUser(User u) {

        return UserResponseDTO.builder()
                .userId(u.getUserId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .phone(u.getPhone())
                .role(u.getRole())
                .status(u.getStatus())
                .build();
    }
}