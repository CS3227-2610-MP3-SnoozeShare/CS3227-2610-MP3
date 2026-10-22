package com.snoozeshare.service;

/** Raised when a requested host block overlaps existing booking or availability data. */
public class AvailabilityConflictException extends RuntimeException {

    private final ConflictType conflictType;

    public AvailabilityConflictException(ConflictType conflictType) {
        super("Date range is not available");
        this.conflictType = conflictType;
    }

    public ConflictType getConflictType() {
        return conflictType;
    }

    /** Identifies which existing availability source prevents the requested block. */
    public enum ConflictType {
        BOOKING,
        HOST_BLOCK
    }
}
