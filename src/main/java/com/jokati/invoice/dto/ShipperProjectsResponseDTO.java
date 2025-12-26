
package com.jokati.invoice.dto;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperProjectsResponseDTO {
    private String id;                     // hex string of ObjectId
    private String userId;
    private String name;
    private Map<String, Object> extra;     // dynamic content
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, Object> project;   // dynamic content
 
    
}
