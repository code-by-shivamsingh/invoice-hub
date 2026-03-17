package com.jokati.invoice.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jokati.invoice.dto.CompanyUserResponseDTO;
import com.jokati.invoice.model.CompanyUser;
import com.jokati.invoice.repository.CompanyUserRepository;

@Service
public class CompanyUserService {

    @Autowired
    private CompanyUserRepository repo;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ================= CREATE =================
    public CompanyUserResponseDTO createUser(CompanyUserResponseDTO dto) {
        validate(dto);

        if (repo.existsByEmailIgnoreCase(dto.getEmail()))
            throw new RuntimeException("Email already exists");

        if (dto.getId() != null && repo.existsById(dto.getId()))
            throw new RuntimeException("ID already exists!");

        CompanyUser user = convertToEntity(dto);

        // Preserve frontend ID if provided
        if (dto.getId() != null)
            user.setId(dto.getId());

        // License calculation
        if (dto.getLicenseStartDate() != null && !dto.getLicenseStartDate().isBlank()) {
            user.setLicenseStartDate(dto.getLicenseStartDate());
            LocalDate start = LocalDate.parse(dto.getLicenseStartDate(), formatter);
            user.setLicenseValidUntil(start.plusYears(1).format(formatter));
        }

        // Assign creator
        user.setCreatedBy(getCurrentUserId());

        return convertToDTO(repo.save(user));
    }

    // ================= UPDATE =================
    public CompanyUserResponseDTO updateUser(String id, CompanyUserResponseDTO dto) {
        String userId = (dto.getId() != null && !dto.getId().isBlank()) ? dto.getId() : id;

        CompanyUser existing = repo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update basic fields
        existing.setEmail(dto.getEmail());
        existing.setCompany(dto.getCompany());
        existing.setFirstName(dto.getFirstName());
        existing.setLastName(dto.getLastName());

        // Update role
        if (dto.getRole() != null)
            existing.setRole(parseRole(dto.getRole()));

        // Update additional permissions
        existing.setMayCreateAdditionalUsers(dto.getMayCreateAdditionalUsers());
        existing.setMaxAdditionalUsers(dto.getMaxAdditionalUsers());

        // Update password safely
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (!dto.getPassword().equals(dto.getRepeatPassword()))
                throw new RuntimeException("Passwords do not match");
            existing.setPassword(dto.getPassword());
            existing.setRepeatPassword(dto.getRepeatPassword());
        }

        // Update modules
        if (dto.getModules() != null) {
            existing.setModules(dto.getModules().stream()
                    .map(value -> {
                        CompanyUser.ModuleNode node = new CompanyUser.ModuleNode();
                        node.setValue(value);
                        return node;
                    }).collect(Collectors.toList()));
        }

        // Update license
        if (dto.getLicenseStartDate() != null && !dto.getLicenseStartDate().isBlank()) {
            existing.setLicenseStartDate(dto.getLicenseStartDate());
            LocalDate start = LocalDate.parse(dto.getLicenseStartDate(), formatter);
            existing.setLicenseValidUntil(start.plusYears(1).format(formatter));
        }

        return convertToDTO(repo.save(existing));
    }

    // ================= DELETE =================
    public String deleteUser(String id) {
        if (!repo.existsById(id))
            return "User already deleted";

        repo.deleteById(id);
        return "Company user deleted successfully";
    }

    // ================= GET BY ID =================
    public CompanyUserResponseDTO getUser(String id) {
        CompanyUser user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    // ================= GET ALL COMPANIES GROUPED =================
    public List<Map<String, Object>> getAllCompaniesWithUsersGrouped() {
        List<CompanyUser> users = repo.findAll();

        // Group users by company
        Map<String, List<CompanyUserResponseDTO>> grouped = users.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getCompany() != null ? u.getCompany() : "Unassigned",
                        LinkedHashMap::new,
                        Collectors.mapping(this::convertToDTO, Collectors.toList())
                ));

        // Convert to List<Map<String,Object>> for response
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((company, userList) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("company", company);
            map.put("users", userList);
            result.add(map);
        });

        return result;
    }

    // ================= DTO ↔ ENTITY =================
    private CompanyUserResponseDTO convertToDTO(CompanyUser user) {
        CompanyUserResponseDTO dto = new CompanyUserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setCompany(user.getCompany());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setLicenseStartDate(user.getLicenseStartDate());
        dto.setLicenseValidUntil(user.getLicenseValidUntil());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);

        dto.setModules(user.getModules() != null
                ? user.getModules().stream()
                      .map(CompanyUser.ModuleNode::getValue)
                      .collect(Collectors.toList())
                : List.of());

        dto.setMayCreateAdditionalUsers(user.getMayCreateAdditionalUsers());
        dto.setMaxAdditionalUsers(user.getMaxAdditionalUsers());

        // Do not expose passwords
        dto.setPassword(null);
        dto.setRepeatPassword(null);

        return dto;
    }

    private CompanyUser convertToEntity(CompanyUserResponseDTO dto) {
        CompanyUser user = new CompanyUser();
        user.setEmail(dto.getEmail());
        user.setCompany(dto.getCompany());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        if (dto.getRole() != null)
            user.setRole(parseRole(dto.getRole()));

        user.setMayCreateAdditionalUsers(dto.getMayCreateAdditionalUsers());
        user.setMaxAdditionalUsers(dto.getMaxAdditionalUsers());

        user.setPassword(dto.getPassword());
        user.setRepeatPassword(dto.getRepeatPassword());

        if (dto.getModules() != null) {
            user.setModules(dto.getModules().stream()
                    .map(value -> {
                        CompanyUser.ModuleNode node = new CompanyUser.ModuleNode();
                        node.setValue(value);
                        return node;
                    }).collect(Collectors.toList()));
        }

        return user;
    }

    // ================= ROLE PARSING =================
    private CompanyUser.CompanyUserRole parseRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) return null;
        switch (roleStr.trim().toUpperCase()) {
            case "USER": return CompanyUser.CompanyUserRole.USER;
            case "ADMIN": return CompanyUser.CompanyUserRole.ADMIN;
            case "SUPERADMIN": return CompanyUser.CompanyUserRole.SUPER_ADMIN;
            default:
                throw new RuntimeException("Invalid role value! Allowed: USER, ADMIN, SUPERADMIN");
        }
    }

    // ================= VALIDATION =================
    private void validate(CompanyUserResponseDTO dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank())
            throw new RuntimeException("Email is required");
        if (dto.getPassword() == null || dto.getPassword().length() < 8)
            throw new RuntimeException("Password must be at least 8 characters");
        if (!dto.getPassword().equals(dto.getRepeatPassword()))
            throw new RuntimeException("Passwords do not match");
    }

    // ================= CURRENT USER ID =================
    private String getCurrentUserId() {
        return "69a27c89769676a1074ebef4"; // Placeholder for authentication
    }

    // ================= CHECK IF ID EXISTS =================
    public boolean existsById(String id) {
        return repo.existsById(id);
    }
}