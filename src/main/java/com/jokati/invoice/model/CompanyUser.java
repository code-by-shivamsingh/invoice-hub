package com.jokati.invoice.model;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class CompanyUser {

    @Id
    private String id; 

    private String email;
    private String company;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    private String licenseStartDate;
    private String licenseValidUntil;

    private CompanyUserRole role;

    private List<ModuleNode> modules;

    private Boolean mayCreateAdditionalUsers;
    private Integer maxAdditionalUsers;

    private String password;
    private String repeatPassword;

    private String createdBy;

    // ================= MODULE NODE =================
    public static class ModuleNode {
        private String value;
        private List<ModuleNode> options;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public List<ModuleNode> getOptions() { return options; }
        public void setOptions(List<ModuleNode> options) { this.options = options; }
    }

    // ================= ROLE ENUM =================
    public enum CompanyUserRole {
        USER,
        ADMIN,
        SUPER_ADMIN
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
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getLicenseStartDate() { return licenseStartDate; }
    public void setLicenseStartDate(String licenseStartDate) { this.licenseStartDate = licenseStartDate; }
    public String getLicenseValidUntil() { return licenseValidUntil; }
    public void setLicenseValidUntil(String licenseValidUntil) { this.licenseValidUntil = licenseValidUntil; }
    public CompanyUserRole getRole() { return role; }
    public void setRole(CompanyUserRole role) { this.role = role; }
    public List<ModuleNode> getModules() { return modules; }
    public void setModules(List<ModuleNode> modules) { this.modules = modules; }
    public Boolean getMayCreateAdditionalUsers() { return mayCreateAdditionalUsers; }
    public void setMayCreateAdditionalUsers(Boolean mayCreateAdditionalUsers) { this.mayCreateAdditionalUsers = mayCreateAdditionalUsers; }
    public Integer getMaxAdditionalUsers() { return maxAdditionalUsers; }
    public void setMaxAdditionalUsers(Integer maxAdditionalUsers) { this.maxAdditionalUsers = maxAdditionalUsers; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRepeatPassword() { return repeatPassword; }
    public void setRepeatPassword(String repeatPassword) { this.repeatPassword = repeatPassword; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}