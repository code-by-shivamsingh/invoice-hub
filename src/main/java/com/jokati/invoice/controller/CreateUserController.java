
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.NewUserRequestDTO;
import com.jokati.invoice.dto.NewUserResponseDTO;
import com.jokati.invoice.service.JokatiUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users API", description = "Create new Jokati users")
public class CreateUserController {
	private static final Logger log = LoggerFactory.getLogger(CreateUserController.class);
    private final JokatiUserService userService;

    public CreateUserController(JokatiUserService userService) {
        this.userService = userService;
    }

    /**
     * POST: mirrors Node logic.
     * - Validates email & password presence.
     * - Calls service to create user.
     * - Returns { statusCode, ...newUserCreated } on success.
     * - Returns { statusCode } on failure.
     */
    @Operation(summary = "Create a new Jokati user")
    @PostMapping
    public ResponseEntity<NewUserResponseDTO> create(@Valid @RequestBody NewUserRequestDTO user) {
    	log.info("Request create : {}",  user);
        try {
            var created = userService.createNewUser(user);
            return ResponseEntity.ok(
                    NewUserResponseDTO.builder()
                            .statusCode(200)
                            .message("User created")
                            .data(created)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    NewUserResponseDTO.builder()
                            .statusCode(500)
                            .message("Error creating user: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }
}
