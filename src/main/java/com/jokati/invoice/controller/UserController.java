
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.UserLoggedInResponseDTO;
import com.jokati.invoice.dto.UserPatchRequestDTO;
import com.jokati.invoice.dto.UserResponseDTO;
import com.jokati.invoice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "APIs to manage Jokati users")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Operation(
        summary = "Get user by firebaseId",
        description = "Returns the first matching user document by firebaseId. Returns 204 if firebaseId is missing/empty.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Found",
                content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "204", description = "No Content")
        }
    )
    @GetMapping
    public ResponseEntity<?> getByFirebaseId(@RequestParam(required = false) String firebaseId) {
    	log.info("Request getByFirebaseId : {}",  firebaseId);
        if (firebaseId == null || firebaseId.isBlank()) {
            // Mirror Node behavior (returns 204 with undefined); we simply return 204
            return ResponseEntity.noContent().build();
        }
        var user = service.findFirstByFirebaseId(firebaseId);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Patch user fields by firebaseId or userId",
        description = "Accepts either firebaseId or userId, and a map of updatedFields. Returns { loggedIn } like the Node API.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Updated",
                content = @Content(schema = @Schema(implementation = UserLoggedInResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (missing id or user not found)")
        }
    )
    @PatchMapping
    public ResponseEntity<UserLoggedInResponseDTO> patchUser(@Valid @RequestBody UserPatchRequestDTO requestDTO) {
    	log.info("Request patchUser : {}",  requestDTO);
    	var result = service.patchUser(requestDTO);
        return ResponseEntity.status(result.getStatus()).body(
                new UserLoggedInResponseDTO(result.isLoggedIn())
        );
    }
}
