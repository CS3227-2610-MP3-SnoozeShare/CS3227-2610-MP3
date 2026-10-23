package com.snoozeshare.controller.guest;

/**
 * Thrown when an ownership or access check fails in a guest controller.
 * Mapped to 403 by {@link GuestExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
