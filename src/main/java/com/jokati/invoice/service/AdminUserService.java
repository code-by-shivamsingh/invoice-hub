package com.jokati.invoice.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.jokati.invoice.dto.AdminUserResponseDTO;
import com.jokati.invoice.dto.AssignModuleDTO;
import com.jokati.invoice.model.AdminUser;
import com.jokati.invoice.repository.AdminUserRepository;

@Service
public class AdminUserService {

    private final AdminUserRepository repo;

    public AdminUserService(AdminUserRepository repo) {
        this.repo = repo;
    }

    // ================= CREATE USER =================
    public AdminUserResponseDTO createUser(AdminUserResponseDTO dto, String creatorId) {

        if (dto.getEmail() == null || dto.getEmail().isBlank())
            throw new RuntimeException("Email is required");

        if (dto.getPassword() == null || dto.getPassword().length() < 8)
            throw new RuntimeException("Password must be at least 8 characters long");

        repo.findByEmail(dto.getEmail())
                .ifPresent(u -> {
                    throw new RuntimeException("Email already exists");
                });

        AdminUser user = convertToEntity(dto);
        user.setCreatedBy(creatorId);

        return convertToDTO(repo.save(user));
    }

    // ================= UPDATE USER =================
    public AdminUserResponseDTO updateUser(String id, AdminUserResponseDTO dto) {

        AdminUser existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        repo.findByEmail(dto.getEmail())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new RuntimeException("Email already exists");
                });

        existing.setName(dto.getName());
        existing.setEmail(dto.getEmail());
        existing.setCompany(dto.getCompany());
        existing.setPhoneNumber(dto.getPhoneNumber());

        if (dto.getPassword() != null && dto.getPassword().length() >= 8)
            existing.setPassword(dto.getPassword());

        if (dto.getRole() != null)
            existing.setRole(safeUserRole(dto.getRole()));

        if (dto.getAssignModules() != null)
            existing.setAssignModules(
                    dto.getAssignModules()
                            .stream()
                            .map(this::convertModuleDTOToEntity)
                            .collect(Collectors.toList())
            );

        return convertToDTO(repo.save(existing));
    }

    // ================= GET USERS =================
    public Map<String, Object> getUsers(Integer page, Integer size) {

        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? size : 10;

        Page<AdminUser> pagedUsers = repo.findAll(PageRequest.of(p, s));

        List<AdminUserResponseDTO> usersList = pagedUsers.getContent()
                .stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())

                
                .filter(u -> u.getRole() != AdminUser.UserRole.SUPER_ADMIN)

                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("users", usersList);

        response.put("pagination", Map.of(
                "pageNumber", pagedUsers.getNumber(),
                "pageSize", pagedUsers.getSize(),
                "totalElements", pagedUsers.getTotalElements(),
                "totalPages", pagedUsers.getTotalPages()
        ));

        return response;
    }

    // ================= GET SINGLE USER =================
    public AdminUserResponseDTO getUser(String id) {

        AdminUser user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

      
        if (user.getRole() == AdminUser.UserRole.SUPER_ADMIN)
            throw new RuntimeException("Access denied");

        return convertToDTO(user);
    }

    // ================= DELETE USER =================
    public String deleteUser(String id) {

        AdminUser user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

       
        if (user.getRole() == AdminUser.UserRole.SUPER_ADMIN)
            throw new RuntimeException("Cannot delete SUPER_ADMIN");

        repo.deleteById(id);

        return "Admin user deleted successfully";
    }

    // ================= ENTITY → DTO =================
    private AdminUserResponseDTO convertToDTO(AdminUser user) {

        AdminUserResponseDTO dto = new AdminUserResponseDTO();

        dto.setId(user.getId());
        dto.setCreatedBy(user.getCreatedBy());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setCompany(user.getCompany());
        dto.setPhoneNumber(user.getPhoneNumber());

        dto.setRole(user.getRole() != null ? user.getRole().name() : "USER");

        if (user.getAssignModules() != null) {
            dto.setAssignModules(
                    user.getAssignModules()
                            .stream()
                            .map(this::convertModuleToDTO)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    // ================= DTO → ENTITY =================
    private AdminUser convertToEntity(AdminUserResponseDTO dto) {

        AdminUser user = new AdminUser();

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setCompany(dto.getCompany());
        user.setPassword(dto.getPassword());
        user.setPhoneNumber(dto.getPhoneNumber());

        if (dto.getRole() != null)
            user.setRole(safeUserRole(dto.getRole()));

        if (dto.getAssignModules() != null) {

            user.setAssignModules(
                    dto.getAssignModules()
                            .stream()
                            .map(this::convertModuleDTOToEntity)
                            .collect(Collectors.toList())
            );
        }

        return user;
    }

    // ================= MODULE ENTITY → DTO =================
    private AssignModuleDTO convertModuleToDTO(AdminUser.ModuleNode node) {

        AssignModuleDTO dto = new AssignModuleDTO();
        dto.setValue(node.getValue());

        if (node.getOptions() != null) {

            dto.setOptions(
                    node.getOptions()
                            .stream()
                            .map(this::convertModuleToDTO)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    // ================= MODULE DTO → ENTITY =================
    private AdminUser.ModuleNode convertModuleDTOToEntity(AssignModuleDTO dto) {

        AdminUser.ModuleNode node = new AdminUser.ModuleNode();
        node.setValue(dto.getValue());

        if (dto.getOptions() != null) {

            node.setOptions(
                    dto.getOptions()
                            .stream()
                            .map(this::convertModuleDTOToEntity)
                            .collect(Collectors.toList())
            );
        }

        return node;
    }

    // ================= SAFE ROLE =================
    private AdminUser.UserRole safeUserRole(String roleStr) {

        if (roleStr == null)
            return AdminUser.UserRole.USER;

        roleStr = roleStr.trim().toUpperCase();

        if (roleStr.equals("SUPERADMIN"))
            roleStr = "SUPER_ADMIN";

        try {
            return AdminUser.UserRole.valueOf(roleStr);
        } catch (Exception e) {
            return AdminUser.UserRole.USER;
        }
    }
}