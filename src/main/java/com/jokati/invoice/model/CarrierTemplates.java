
package com.jokati.invoice.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "carrier-templates")
public class CarrierTemplates {

    @Id
    private String id;

    /**
     * Flexible structure to mirror Mongoose { strict:false }.
     * Store entire template payload in a map.
     */
    private Map<String, Object> template;
}
