package com.snoozeshare.service;

/** A requested domain resource does not exist. */
public class NotFoundException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }
}
