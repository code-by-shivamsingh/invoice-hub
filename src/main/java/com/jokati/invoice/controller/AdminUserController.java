package com.jokati.invoice.controller;

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

      
        return ResponseEntity.ok(createdUser);
    }

    // ================= GET USERS =================
    @GetMapping
    public ResponseEntity<Map<String,Object>> getUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Map<String,Object> response = adminUserService.getUsers(page, size);

        // Return only users + pagination
        return ResponseEntity.ok(response);
    }

    // ================= GET SINGLE USER =================
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponseDTO> getUser(@PathVariable String id) {

        AdminUserResponseDTO user = adminUserService.getUser(id);

        // Return only the user DTO
        return ResponseEntity.ok(user);
    }

    // ================= UPDATE USER =================
    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponseDTO> updateUser(
            @PathVariable String id,
            @RequestBody AdminUserResponseDTO dto) {

        AdminUserResponseDTO updatedUser = adminUserService.updateUser(id, dto);

        
        return ResponseEntity.ok(updatedUser);
    }

    // ================= DELETE USER =================
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String id) {

        String message = adminUserService.deleteUser(id);

  
        return ResponseEntity.ok(Map.of("message", message));
    }

    // ================= GLOBAL ERROR HANDLER =================
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {

        return ResponseEntity.badRequest().body(
                Map.of(
                        "status", "error",
                        "message", ex.getMessage()
                )
        );
    }
}