package com.jokati.invoice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jokati.invoice.dto.AdminUserResponseDTO;
import com.jokati.invoice.service.AdminUserService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@CrossOrigin
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    // ================= CREATE USER =================
    @PostMapping
    public ResponseEntity<AdminUserResponseDTO> createUser(@RequestBody AdminUserResponseDTO dto) {

        String creatorId = "69a27c89769676a1074ebef4";

        AdminUserResponseDTO createdUser = adminUserService.createUser(dto, creatorId);

        // Return only the created user
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    // ================= GET USERS =================
    @GetMapping
    public ResponseEntity<?> getUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Map<String,Object> response =
                adminUserService.getUsers(page, size);

        return ResponseEntity.ok(response);
    }

    // ================= GET SINGLE USER =================
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {

        AdminUserResponseDTO user =
                adminUserService.getUser(id);

        return ResponseEntity.ok(Map.of(
                "user", user
        ));
    }

    // ================= UPDATE USER =================
    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponseDTO> updateUser(
            @PathVariable String id,
            @RequestBody AdminUserResponseDTO dto) {

        AdminUserResponseDTO updatedUser = adminUserService.updateUser(id, dto);

        // Return only the updated user
        return ResponseEntity.ok(updatedUser);
    }

    // ================= DELETE USER =================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {

        String message =
                adminUserService.deleteUser(id);

        return ResponseEntity.ok(Map.of(
                "message", message
        ));
    }

    // ================= GLOBAL ERROR HANDLER =================
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "status", "error",
                        "message", ex.getMessage()
                )
        );
    }
}