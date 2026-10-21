package com.snoozeshare.domain.policy;

import java.time.Clock;
import java.time.LocalDate;

/** The one definition of which stay and search dates are acceptable; the UI only mirrors it. */
public final class StayDatePolicy {

    private final Clock clock;

    public StayDatePolicy(Clock clock) {
        this.clock = clock;
    }

    /** A booking needs both dates, a start of today or later, and an end after the start. */
    public void requireValidStay(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (start.isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("EndDate must be after startDate");
        }
    }

    /** A search may leave either date open, but a given date must not be past and a range must not be inverted. */
    public void requireValidSearchRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        if (from != null && from.isBefore(today)) {
            throw new IllegalArgumentException("Check-in cannot be in the past");
        }
        if (to != null && to.isBefore(today)) {
            throw new IllegalArgumentException("Check-out cannot be in the past");
        }
        if (from != null && to != null && !to.isAfter(from)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
    }

    /** A date a user picks for something that has not happened yet must be today or later. */
    public void requireNotInPast(LocalDate date, String label) {
        if (date != null && date.isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException(label + " cannot be in the past");
        }
    }
}
