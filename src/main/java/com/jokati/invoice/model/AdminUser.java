package com.jokati.invoice.model;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonInclude;

@Document(collection = "users")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUser {

    @Id
    private String id;
    private String createdBy;
    private String name;
    private String email;
    private String company;   
    private String password;
    private String phoneNumber;
    private UserRole role;
    private List<ModuleNode> assignModules;

    // ========= UserRole Enum =========
    public enum UserRole {
        ADMIN,
        USER,
        MANAGER, SUPER_ADMIN
    }

    // ========= Nested module structure =========
    public static class ModuleNode {
        private String value;
        private List<ModuleNode> options;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public List<ModuleNode> getOptions() { return options; }
        public void setOptions(List<ModuleNode> options) { this.options = options != null ? options : List.of(); }
    }

    // ========= Getters & Setters =========
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public List<ModuleNode> getAssignModules() { return assignModules; }
    public void setAssignModules(List<ModuleNode> assignModules) { this.assignModules = assignModules != null ? assignModules : List.of(); }
}