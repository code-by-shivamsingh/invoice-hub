
package com.jokati.invoice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String firebaseId; // or identity provider id
    private String email;
    private String company;
    private String firstName;
    private String lastName;

    private Boolean loggedIn;
    private Date created;
}