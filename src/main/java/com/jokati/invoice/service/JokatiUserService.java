
package com.jokati.invoice.service;

import com.jokati.invoice.dto.CarrierAddressDTO;
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
        return Map.of(
                "email", user.getEmail(),
                "company", user.getCompany(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "status", "created"
        );
    }

    // Overload for existing controller call
    public Map<String, Object> createNewUser(CarrierAddressDTO carrier, String initialPassword) {
        NewUserRequestDTO user = new NewUserRequestDTO();
        user.setEmail(carrier.getEmail());      // adjust to your DTO
        user.setPassword(initialPassword);
        user.setCompany(carrier.getCompany());     // adjust to your DTO
        user.setFirstName(carrier.getFirstName());
        user.setLastName(carrier.getLastName());
        return createNewUser(user);
    }

}
