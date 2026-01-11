package com.jokati.invoice.model;


import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "email-templates")
public class EmailTemplate {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;      // e.g., "angebot-erhalten"

    private String subject;   // supports {{key}} and {{{key}}}
    private String body;      // HTML with placeholders

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
