package com.jokati.invoice.service;

import org.bson.types.ObjectId;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.jokati.invoice.DuplicateUserException;
import com.jokati.invoice.dto.*;
import com.jokati.invoice.model.User;
import com.jokati.invoice.repository.UserRepository;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;
 // ✅ CREATE USER
    public UserResponseDTO createUser(UserRequestDTO dto) {
        try {
            // Password validation
            if (dto.getPassword() == null || dto.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password must be provided");
            }
            int len = dto.getPassword().length();
            if (len < 6 || len > 8) {
                throw new IllegalArgumentException("Password must be between 6 and 8 characters");
            }

            User user = User.builder()
                    .userId(dto.getUserId())
                    .email(dto.getEmail() != null ? dto.getEmail().toLowerCase() : null)
                    .firstName(dto.getFirstName())
                    .lastName(dto.getLastName())
                    .phone(dto.getPhone())
                    .role(dto.getRole())
                    .companyId(dto.getCompanyId())
                    .modules(dto.getModules())
                    .allowCreateUsers(dto.getAllowCreateUsers())
                    .maxCreatableUsers(dto.getMaxCreatableUsers())
                    .status(dto.getStatus()) 
                    .password(passwordEncoder.encode(dto.getPassword())) 
                    .createdBy(dto.getCreatedBy())
                    .build();

            User savedUser = repository.save(user);
            return toDTO(savedUser);

        } catch (Exception ex) {
            if (ex instanceof org.springframework.dao.DuplicateKeyException ||
                ex instanceof org.springframework.dao.DataIntegrityViolationException ||
                (ex.getCause() != null && ex.getCause() instanceof MongoWriteException)) {
                throw new DuplicateUserException("UserId or Email already exists");
            }
            throw ex;
        }
    }
    // ✅ GET ALL USERS
    public List<UserResponseDTO> getAllUsersList() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    // ✅ Get user by ID, 
    public UserResponseDTO getById(String id) {
        User user = repository.findById(new ObjectId(id))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDTO(user);
    }

    // ✅ UPDATE USER
    public UserResponseDTO updateUser(String id, UserRequestDTO dto) {
        User user = repository.findById(new ObjectId(id))
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setCompanyId(dto.getCompanyId());
        user.setModules(dto.getModules());
        user.setAllowCreateUsers(dto.getAllowCreateUsers());
        user.setMaxCreatableUsers(dto.getMaxCreatableUsers());
        user.setStatus(dto.getStatus());

        User updatedUser = repository.save(user);
        return toDTO(updatedUser);
    }

    // ✅ DELETE USER
    public boolean deleteUser(String id) {
        ObjectId objId = new ObjectId(id);
        if (!repository.existsById(objId)) return false;
        repository.deleteById(objId);
        return true;
    }

    // ✅ MAPPER: User -> UserResponseDTO
    private UserResponseDTO toDTO(User u) {
        // Recursive copy of modules
        List<ModuleNode> modules = null;
        if (u.getModules() != null) {
            modules = u.getModules().stream()
                    .map(this::copyModule) 
                    .toList();
        }

        return UserResponseDTO.builder()
                .id(u.getId() != null ? u.getId().toHexString() : null)
                .userId(u.getUserId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .phone(u.getPhone())
                .role(u.getRole())
                .companyId(u.getCompanyId())
                .modules(modules)
                .allowCreateUsers(u.getAllowCreateUsers())
                .maxCreatableUsers(u.getMaxCreatableUsers())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .createdBy(u.getCreatedBy())
                .build();
    }

    // ✅ Recursive copy of ModuleNode
    private ModuleNode copyModule(ModuleNode m) {
        List<ModuleNode> options = null;
        if (m.getOptions() != null) {
            options = m.getOptions().stream()
                    .map(this::copyModule)
                    .toList();
        }
        return ModuleNode.builder()
                .value(m.getValue())
                .options(options)
                .build();
    }
}