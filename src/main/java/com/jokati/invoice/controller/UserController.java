
package com.jokati.invoice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.UserLoggedInResponseDTO;
import com.jokati.invoice.dto.UserPatchRequestDTO;
import com.jokati.invoice.dto.UserResponseDTO;
import com.jokati.invoice.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
// Avoid importing io.swagger.v3.oas.annotations.responses.ApiResponse due to name collision
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
            @io.swagger.v3.oas.annotations.responses.ApiResponse( // fully-qualified to avoid collision
                responseCode = "200",
                description = "Found",
                content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "204",
                description = "No Content"
            )
        }
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getByFirebaseId(@RequestParam(required = false) String firebaseId) {
        log.info("Request getByFirebaseId : {}", firebaseId);
        if (firebaseId == null || firebaseId.isBlank()) {
            // Mirror Node behavior (returns 204 with undefined)
            return ResponseUtil.noContent("No Content");
        }
        var user = service.findFirstByFirebaseId(firebaseId);
        if (user == null) {
            return ResponseUtil.noContent("No Content");
        }
        return ResponseUtil.ok(user, "User fetched successfully");
    }

    @Operation(
        summary = "Patch user fields by firebaseId or userId",
        description = "Accepts either firebaseId or userId, and a map of updatedFields. Returns { loggedIn } like the Node API, with 200 or 401.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Updated",
                content = @Content(schema = @Schema(implementation = UserLoggedInResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized (missing id or user not found)"
            )
        }
    )
    @PatchMapping
    public ResponseEntity<ApiResponse<UserLoggedInResponseDTO>> patchUser(
            @Valid @RequestBody UserPatchRequestDTO requestDTO) {
        log.info("Request patchUser : {}", requestDTO);

        var result = service.patchUser(requestDTO);
        var body = new UserLoggedInResponseDTO(result.isLoggedIn());

        // Mirror Node: status can be 200 or 401
        HttpStatus status = (result.getStatus() == 200) ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseUtil.withStatus(status, status == HttpStatus.OK ? "User patched" : "Unauthorized", body);
    }
}
