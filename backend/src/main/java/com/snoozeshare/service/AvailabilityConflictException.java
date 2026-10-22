package com.snoozeshare.service;

/** Raised when a requested host block overlaps existing booking or availability data. */
public class AvailabilityConflictException extends RuntimeException {

    public AvailabilityConflictException() {
        super("Date range is not available");
    }
}
