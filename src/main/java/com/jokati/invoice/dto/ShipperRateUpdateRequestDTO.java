package com.jokati.invoice.dto;

import java.util.List;

public class ShipperRateUpdateRequestDTO {

    private String projectId;
    private String countryCode;
    private List<ShipperRateUpdateItemDTO> updates;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public List<ShipperRateUpdateItemDTO> getUpdates() {
        return updates;
    }

    public void setUpdates(List<ShipperRateUpdateItemDTO> updates) {
        this.updates = updates;
    }
}