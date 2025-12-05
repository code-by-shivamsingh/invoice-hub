
package com.jokati.invoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewUserResponseDTO {
    private int statusCode;
    private String message;
    private Object data; // e.g., created user payload or identity provider response
}