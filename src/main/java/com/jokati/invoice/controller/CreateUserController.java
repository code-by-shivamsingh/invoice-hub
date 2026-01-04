
package com.jokati.invoice.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.NewUserRequestDTO;
import com.jokati.invoice.service.JokatiUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users API", description = "Create new Jokati users")
public class CreateUserController {
    private static final Logger log = LoggerFactory.getLogger(CreateUserController.class);
    private final JokatiUserService userService;

    public CreateUserController(JokatiUserService userService) {
        this.userService = userService;
    }

    /**
     * POST: mirrors Node logic.
     * - Validates email & password presence (Bean Validation).
     * - Calls service to create user.
     * - Returns standardized ApiResponse envelope.
     */
    @Operation(summary = "Create a new Jokati user")
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@Valid @RequestBody NewUserRequestDTO user) {
        // Avoid logging sensitive data like password
        log.info("Create user request: email={}, company={}, firstName={}, lastName={}",
                user.getEmail(), user.getCompany(), user.getFirstName(), user.getLastName());

        var created = userService.createNewUser(user);
        // 200 OK on success (mirror your Node behavior)
        return ResponseUtil.ok(created, "User created");
    }
}
