package com.jokati.invoice.dto;

import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String password; 
    private String role;
    private String companyId;

    private List<ModuleNode> modules; 

    private Boolean allowCreateUsers;
    private Integer maxCreatableUsers;
    private String status;

    // ✅ Add this field
    private String createdBy;
}