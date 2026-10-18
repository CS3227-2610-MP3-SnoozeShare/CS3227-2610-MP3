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

    /** A search may leave either date open, but a given range must not be inverted. */
    public void requireValidSearchRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
    }
}
