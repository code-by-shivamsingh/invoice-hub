package com.jokati.invoice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jokati.invoice.model.CompanyUser.CompanyUserRole;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id","email","company","firstName","lastName",
        "licenseStartDate","licenseValidUntil",
        "modules","users","role",
        "mayCreateAdditionalUsers","maxAdditionalUsers",
        "password","repeatPassword"
})
public class CompanyUserResponseDTO {

    private String id;
    private String email;
    private String company;
    private String firstName;
    private String lastName;
    private String licenseStartDate;
    private String licenseValidUntil;

    private String role;

    private List<String> modules;
    private List<UserDTO> users;

    private Boolean mayCreateAdditionalUsers;
    private Integer maxAdditionalUsers;

    private String password;
    private String repeatPassword;

    // ================= ROLE =================

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public CompanyUserRole getRoleEnum() {
        if (role == null || role.isEmpty()) {
            return null;
        }

        try {
            return CompanyUserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid role value! Allowed values: USER, ADMIN, SUPER_ADMIN"
            );
        }
    }

    public void setRoleEnum(CompanyUserRole roleEnum) {
        this.role = roleEnum != null ? roleEnum.name() : null;
    }

    // ================= GETTERS & SETTERS =================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getLicenseStartDate() { return licenseStartDate; }
    public void setLicenseStartDate(String licenseStartDate) {
        this.licenseStartDate = licenseStartDate;
    }

    public String getLicenseValidUntil() { return licenseValidUntil; }
    public void setLicenseValidUntil(String licenseValidUntil) {
        this.licenseValidUntil = licenseValidUntil;
    }

    public List<String> getModules() { return modules; }
    public void setModules(List<String> modules) { this.modules = modules; }

    public List<UserDTO> getUsers() { return users; }
    public void setUsers(List<UserDTO> users) { this.users = users; }

    public Boolean getMayCreateAdditionalUsers() {
        return mayCreateAdditionalUsers;
    }

    public void setMayCreateAdditionalUsers(Boolean mayCreateAdditionalUsers) {
        this.mayCreateAdditionalUsers = mayCreateAdditionalUsers;
    }

    public Integer getMaxAdditionalUsers() {
        return maxAdditionalUsers;
    }

    public void setMaxAdditionalUsers(Integer maxAdditionalUsers) {
        this.maxAdditionalUsers = maxAdditionalUsers;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRepeatPassword() { return repeatPassword; }
    public void setRepeatPassword(String repeatPassword) {
        this.repeatPassword = repeatPassword;
    }

    // ================= USER DTO =================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserDTO {

        private String id;
        private String name;
        private String email;
        private String phoneNumber;
        private String role;

        private List<ModuleDTO> assignModules;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class ModuleDTO {

            private String value;
            private List<ModuleDTO> options;

            public String getValue() { return value; }
            public void setValue(String value) { this.value = value; }

            public List<ModuleDTO> getOptions() { return options; }
            public void setOptions(List<ModuleDTO> options) { this.options = options; }
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public List<ModuleDTO> getAssignModules() {
            return assignModules;
        }

        public void setAssignModules(List<ModuleDTO> assignModules) {
            this.assignModules = assignModules;
        }
    }
}