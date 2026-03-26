package com.jokati.invoice.dto;

import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDTO {
    
    private String id;                 
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private String companyId;

    private List<ModuleNode> modules;   

    private Boolean allowCreateUsers;
    private Integer maxCreatableUsers;
    private String status;

    private Instant createdAt;         
    private Instant updatedAt;

    // ✅ Add this field
    private String createdBy;

	public String Password;
		
	   
}