package com.jokati.invoice.dto;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPatchRequestDTO {

    @NotNull(message = "id is required")
    private IdDTO id;

    @NotEmpty(message = "updatedFields cannot be empty")
    private Map<String, Object> updatedFields;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IdDTO {
        private String firebaseId;
        private String userId;
    }
}