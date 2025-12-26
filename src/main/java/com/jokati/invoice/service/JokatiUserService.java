
package com.jokati.invoice.service;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.jokati.invoice.common.ErrorCodes;
import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.dto.CarrierAddressDTO;
import com.jokati.invoice.dto.NewUserRequestDTO;
import com.jokati.invoice.exception.ApiException;

@Service
public class JokatiUserService {

    // Simple email regex (you may replace with more robust validation or rely on @Email)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Map<String, Object> createNewUser(NewUserRequestDTO user) {
        // Domain validations (beyond Bean Validation)
        if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.VALIDATION_ERROR,
                    "Invalid email format",
                    java.util.List.of(new ErrorDetail(ErrorCodes.VALIDATION_ERROR, "Invalid email format", "email", user.getEmail()))
            );
        }

        if (user.getPassword() != null && user.getPassword().length() < 8) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.VALIDATION_ERROR,
                    "Password must be at least 8 characters",
                    java.util.List.of(new ErrorDetail(ErrorCodes.VALIDATION_ERROR, "Password must be at least 8 characters", "password", null))
            );
        }

        // TODO: Replace with actual identity creation logic (Firebase/Keycloak/custom DB)
        // If external provider fails, throw ApiException with appropriate status & code

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
        user.setEmail(carrier.getEmail());   // map appropriately from CarrierAddressDTO
        user.setPassword(initialPassword);
        user.setCompany(carrier.getCompany());
        user.setFirstName(carrier.getFirstName());
        user.setLastName(carrier.getLastName());
        return createNewUser(user);
    }
}

