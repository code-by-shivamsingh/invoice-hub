package com.jokati.invoice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jokati.invoice.dto.*;
import com.jokati.invoice.service.UserService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users-create")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    // CREATE USER
    @PostMapping
    public ResponseEntity<?> create(@RequestBody UserRequestDTO dto) {
        try {
            UserResponseDTO createdUser = service.createUser(dto);
            return ResponseEntity.ok(Map.of("users", List.of(createdUser)));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(409) 
                    .body(Map.of(
                            "success", false,
                            "message", ex.getMessage()
                    ));
        }
    }
    // GET ALL USERS
    @GetMapping
    public ResponseEntity<Map<String, List<UserResponseDTO>>> getAll() {
        List<UserResponseDTO> allUsers = service.getAllUsersList(); 
        return ResponseEntity.ok(Map.of("users", allUsers));
    }

  
    // ✅ GET USER BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            UserResponseDTO user = service.getById(id);
            return ResponseEntity.ok(Map.of("users", List.of(user)));
        } catch (RuntimeException ex) {
            if ("User not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404)
                        .body(Map.of(
                                "success", false,
                                "message", ex.getMessage()
                        ));
            }
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "message", "Unexpected server error"
                    ));
        }
    }

    // UPDATE USER
    
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id,
                                    @RequestBody UserRequestDTO dto) {
        try {
            UserResponseDTO updatedUser = service.updateUser(id, dto);
            return ResponseEntity.ok(Map.of("users", List.of(updatedUser)));
        } catch (RuntimeException ex) {
            if ("User not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404)
                        .body(Map.of(
                                "success", false,
                                "message", ex.getMessage()
                        ));
            }
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "message", "Unexpected server error"
                    ));
        }
    }

    // DELETE USER
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        boolean deleted = service.deleteUser(id);
        if (!deleted) return ResponseEntity.status(404).body("User not found");
        return ResponseEntity.ok("User deleted successfully");
    }
}