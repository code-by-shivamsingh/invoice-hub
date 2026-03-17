package com.jokati.invoice.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "value", "options" })
public class AssignModuleDTO {

    private String value;
    private List<AssignModuleDTO> options;

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public List<AssignModuleDTO> getOptions() { return options; }
    public void setOptions(List<AssignModuleDTO> options) { this.options = options; }
}