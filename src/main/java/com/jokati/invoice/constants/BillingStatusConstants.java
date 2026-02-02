package com.jokati.invoice.constants;

import com.jokati.invoice.model.StatusInfo;

public final class BillingStatusConstants {

    private BillingStatusConstants() {}

    // System calculated statuses 
    public static final String CORRECT_BILLING     = "Correct billing";
    public static final String INCORRECT_BILLING   = "Incorrect billing";
    public static final String ACCEPTED            = "Accepted";
    public static final String TOLERANCE_ACCEPTED  = "Tolerance accepted";

    // Manual decision statuses 
    public static final String MANUALLY_ACCEPTED   = "MANUALLY_ACCEPTED";
    public static final String MANUALLY_REJECTED   = "MANUALLY_REJECTED";

    // Colors 
    public static final String COLOR_SUCCESS = "#28a745";
    public static final String COLOR_ERROR   = "#dc3545";
    public static final String COLOR_WARNING = "#ffc107";

    //  Helper methods  
    public static StatusInfo manualAcceptedStatus() {
        return StatusInfo.builder()
                .label(MANUALLY_ACCEPTED)
                .color(COLOR_SUCCESS)
                .build();
    }

    public static StatusInfo manualRejectedStatus() {
        return StatusInfo.builder()
                .label(MANUALLY_REJECTED)
                .color(COLOR_ERROR)
                .build();
    }
}
