
package com.jokati.invoice.service;

import com.jokati.invoice.dto.NewUserRequestDTO;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service that mirrors Node's create-new-jokati-user action.
 * Plug in your actual user creation (e.g., Firebase Admin SDK / Keycloak / custom DB).
 */
@Service
public class JokatiUserService {

    public Map<String, Object> createNewUser(NewUserRequestDTO user) {
        // TODO: Implement actual identity creation logic.
        // For now, we mimic a success response structure.
        return Map.of(
                "email", user.getEmail(),
                "company", user.getCompany(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "status", "created"
        );
    }
}
