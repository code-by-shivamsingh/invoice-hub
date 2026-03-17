package com.jokati.invoice.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({
        "createdBy",
        "id",
        "name",
        "email",
        "company",
        "password",
        "phoneNumber",
        "role",
        "assignModules"
})
public class AdminUserResponseDTO {

    private String createdBy;
    private String id;
    private String name;
    private String email;
    private String company;
    private String password;
    private String phoneNumber;
    private String role;
    private List<AssignModuleDTO> assignModules;

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<AssignModuleDTO> getAssignModules() {
        return assignModules;
    }

    public void setAssignModules(List<AssignModuleDTO> assignModules) {
        this.assignModules = assignModules;
    }
}