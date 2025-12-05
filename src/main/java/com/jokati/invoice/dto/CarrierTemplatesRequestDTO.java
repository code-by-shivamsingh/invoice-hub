
package com.jokati.invoice.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CarrierTemplatesRequestDTO {
    private Map<String, Object> template; // whole payload
}
