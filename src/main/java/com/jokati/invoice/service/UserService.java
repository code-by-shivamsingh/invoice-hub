
package com.jokati.invoice.service;

import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jokati.invoice.dto.UserPatchRequestDTO;
import com.jokati.invoice.dto.UserResponseDTO;
import com.jokati.invoice.model.User;
import com.jokati.invoice.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.var;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    public UserResponseDTO findFirstByFirebaseId(String firebaseId) {
        var list = repository.findByFirebaseId(firebaseId);
        if (list == null || list.isEmpty()) return null;
        var user = list.get(0);
        return toResponseDTO(user);
    }

    /**
     * Patch user by either firebaseId or userId.
     * Returns a result containing http status and the loggedIn flag (mirroring Node).
     */
    @Transactional
    public PatchResult patchUser(UserPatchRequestDTO request) {
        boolean loggedIn = false;

        // Validate IDs
        String firebaseId = request.getId() != null ? request.getId().getFirebaseId() : null;
        String userId = request.getId() != null ? request.getId().getUserId() : null;

        if ((firebaseId == null || firebaseId.isBlank()) && (userId == null || userId.isBlank())) {
            // Node returns 401 { loggedIn: false } when no ID is provided
            return new PatchResult(401, false);
        }

        // Select target user
        User target = null;
        if (firebaseId != null && !firebaseId.isBlank()) {
            var matches = repository.findByFirebaseId(firebaseId);
            if (!matches.isEmpty()) {
                target = matches.get(0);
            }
        } else if (userId != null && !userId.isBlank()) {
            var id = new ObjectId(userId);
            target = repository.findById(id).orElse(null);
        }

        if (target == null) {
            // Node returns 401 when no document found
            return new PatchResult(401, false);
        }

        // Apply updated fields (dynamic map, similar to Node's {strict:false})
        applyUpdates(target, request.getUpdatedFields());

        // Save and return loggedIn flag
        var saved = repository.save(target);
        loggedIn = Boolean.TRUE.equals(saved.getLoggedIn());

        return new PatchResult(200, loggedIn);
    }

    /* -------- helpers -------- */

    private void applyUpdates(User user, Map<String, Object> updatedFields) {
        if (updatedFields == null || updatedFields.isEmpty()) return;

        updatedFields.forEach((key, value) -> {
            switch (key) {
                case "firebaseId" -> user.setFirebaseId(asString(value));
                case "email" -> user.setEmail(asString(value));
                case "company" -> user.setCompany(asString(value));
                case "firstName" -> user.setFirstName(asString(value));
                case "lastName" -> user.setLastName(asString(value));
                case "loggedIn" -> user.setLoggedIn(asBoolean(value));
                case "created" -> user.setCreated(asInstant(value));
                default -> {
                    // Ignore unknown fields or extend with extra map if you need strict:false behavior
                }
            }
        });
    }

    private String asString(Object v) { return v == null ? null : String.valueOf(v); }
    private Boolean asBoolean(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(v));
    }
    private java.time.Instant asInstant(Object v) {
        if (v == null) return null;
        if (v instanceof java.time.Instant i) return i;
        try { return java.time.Instant.parse(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private UserResponseDTO toResponseDTO(User u) {
        return UserResponseDTO.builder()
                .id(u.getId() != null ? u.getId().toHexString() : null)
                .firebaseId(u.getFirebaseId())
                .email(u.getEmail())
                .company(u.getCompany())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .loggedIn(Boolean.TRUE.equals(u.getLoggedIn()))
                .created(u.getCreated())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    @Getter
    @AllArgsConstructor
    public static class PatchResult {
        private final int status;
        private final boolean loggedIn;
    }
}
