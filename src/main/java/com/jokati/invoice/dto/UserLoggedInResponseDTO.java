
package com.jokati.invoice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoggedInResponseDTO {
    private boolean loggedIn;
}
