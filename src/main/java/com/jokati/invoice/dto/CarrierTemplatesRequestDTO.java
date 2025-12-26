
package com.jokati.invoice.dto;

import java.util.Map;

import lombok.Data;

@Data
public class CarrierTemplatesRequestDTO {
    private Map<String, Object> template; // whole payload
}
