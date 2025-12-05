
package com.jokati.invoice.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private String id;
    private String firebaseId;
    private String email;
    private String company;
    private String firstName;
    private String lastName;
    private Boolean loggedIn;
    private Instant created;
    private Instant createdAt; // from auditing if enabled
    private Instant updatedAt; // from auditing if enabled
}