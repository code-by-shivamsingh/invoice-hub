
package com.jokati.invoice.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ApiError", description = "Error object with code and message")
public class ApiError {

    @Schema(description = "Machine-readable error code", example = "VALIDATION_ERROR")
    private String code;

    @Schema(description = "Human-readable error message", example = "Field 'userId' must not be blank")
    private String message;

    @Schema(description = "Optional field name causing the error", example = "userId")
    private String field;
}
