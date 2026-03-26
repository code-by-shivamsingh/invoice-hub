package com.jokati.invoice.dto;

import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleNode {
    private String value;
    private List<ModuleNode> options; 
}