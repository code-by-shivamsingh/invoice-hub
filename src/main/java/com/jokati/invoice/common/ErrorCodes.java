
package com.jokati.invoice.common;

public final class ErrorCodes {
    private ErrorCodes() {}

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String MALFORMED_JSON = "MALFORMED_JSON";
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    public static final String MISSING_PARAM = "MISSING_PARAM";
    public static final String MISSING_PATH_VARIABLE = "MISSING_PATH_VARIABLE";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String CONFLICT = "CONFLICT";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
	public static final String EMAIL_ERROR = "EMAIL_FAILED";
}
