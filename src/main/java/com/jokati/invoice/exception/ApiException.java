
package com.jokati.invoice.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

import com.jokati.invoice.common.ErrorDetail;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final List<ErrorDetail> errors;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = null;
    }

    public ApiException(HttpStatus status, String code, String message, List<ErrorDetail> errors) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = errors;
    }
}
