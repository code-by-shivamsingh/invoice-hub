package com.jokati.invoice.model;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "users")
public class Company {

    @Id
    private ObjectId id;

    private String companyId;
    private String companyName;

    private LocalDate licenseStart;
    private LocalDate licenseEnd;

    private List<String> modules;

    private Boolean mayCreateAdditionalUsers;
    private Integer maxAdditionalUsers;

    private Integer totalUsers;
    private Integer activeUsers;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}