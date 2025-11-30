
package com.jokati.invoice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "diesel-floater")
public class DieselFloater {
    @Id
    private String id;
    private Map<String, Object> years; // Flexible structure for nested data
}
