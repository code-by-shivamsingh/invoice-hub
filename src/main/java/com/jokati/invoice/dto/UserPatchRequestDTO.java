
package com.jokati.invoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * Mirrors Node PATCH body:
 * {
 *   "id": { "firebaseId": "...", "userId": "..." },
 *   "updatedFields": { ... }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPatchRequestDTO {

    @NotNull
    private IdDTO id;

    @NotNull
    private Map<String, Object> updatedFields;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IdDTO {
        private String firebaseId;  // optional
        private String userId;      // optional (hex string of ObjectId)
    }
}
