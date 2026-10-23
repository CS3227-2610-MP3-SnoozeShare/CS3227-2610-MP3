package com.snoozeshare.api.error;

import org.springframework.http.HttpStatus;

/** An API-safe Host failure with a stable HTTP status and machine-readable code. */
public class HostApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public HostApiException(HttpStatus status, String code) {
        super(code);
        if (status == null || code == null || code.isBlank()) {
            throw new IllegalArgumentException("A status and stable error code are required");
        }
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
