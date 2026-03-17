package com.jokati.invoice.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import com.jokati.invoice.dto.CompanyUserResponseDTO;
import com.jokati.invoice.service.CompanyUserService;

@RestController
@RequestMapping("/api/v1/company")
@CrossOrigin
public class CompanyUserController {

    @Autowired
    private CompanyUserService service;

    // ================= CREATE =================
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CompanyUserResponseDTO user) {
        try {
            validateRole(user);

            // Check if ID already exists
            if (user.getId() != null && !user.getId().isBlank() && service.existsById(user.getId())) {
                return error("ID already exists in database!");
            }

            CompanyUserResponseDTO createdUser = service.createUser(user);
            return ResponseEntity.ok(createdUser);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    // ================= UPDATE =================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id,
                                        @RequestBody CompanyUserResponseDTO user) {
        try {
            validateRole(user);
            CompanyUserResponseDTO updatedUser = service.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    // ================= GET ALL =================
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<Map<String, Object>> users = service.getAllCompaniesWithUsersGrouped();
            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    // ================= GET BY ID =================
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        try {
            CompanyUserResponseDTO user = service.getUser(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    // ================= DELETE =================
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String id) {
        try {
            String message = service.deleteUser(id);
            Map<String, String> res = new HashMap<>();
            res.put("message", message);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    // ================= COMMON ERROR RESPONSE =================
    private ResponseEntity<Map<String, String>> error(String msg) {
        Map<String, String> res = new HashMap<>();
        res.put("message", msg);
        return ResponseEntity.badRequest().body(res);
    }

    // ================= ROLE VALIDATION =================
    private void validateRole(CompanyUserResponseDTO user) {
        if (user.getRole() == null || user.getRole().isBlank())
            throw new IllegalArgumentException("Role is required! Allowed: USER, ADMIN, SUPERADMIN");

        String role = user.getRole().trim().toUpperCase();
        if (!role.equals("USER") && !role.equals("ADMIN") && !role.equals("SUPERADMIN"))
            throw new IllegalArgumentException("Invalid role value! Allowed: USER, ADMIN, SUPERADMIN");
    }
}