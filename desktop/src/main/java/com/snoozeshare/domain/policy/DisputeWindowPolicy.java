package com.snoozeshare.domain.policy;

import java.time.Clock;
import java.time.LocalDate;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.model.Booking;

/**
 * The one definition of the dispute window. Filing stays open through {@link #WINDOW_DAYS} days after check-out;
 * auto-completion (which pays the host) is allowed only once that window has closed, so an accepted complaint
 * always reaches a booking that can still be settled.
 */
public final class DisputeWindowPolicy {

    public static final int WINDOW_DAYS = 7;

    private final Clock clock;

    public DisputeWindowPolicy(Clock clock) {
        this.clock = clock;
    }

    /** CONFIRMED or COMPLETED, trip started, and not later than check-out plus {@value #WINDOW_DAYS} days. */
    public boolean canFileTicket(Booking booking) {
        LocalDate today = LocalDate.now(clock);
        boolean statusEligible = booking.status() == BookingStatus.CONFIRMED
                || booking.status() == BookingStatus.COMPLETED;
        return statusEligible && !today.isBefore(booking.startDate())
                && !today.isAfter(booking.endDate().plusDays(WINDOW_DAYS));
    }

    /** A CONFIRMED booking whose filing window has closed. */
    public boolean canAutoComplete(Booking booking) {
        return booking.status() == BookingStatus.CONFIRMED
                && LocalDate.now(clock).isAfter(booking.endDate().plusDays(WINDOW_DAYS));
    }

    /** The latest check-out date the completion sweep may complete today. */
    public LocalDate completionCutoff() {
        return LocalDate.now(clock).minusDays(WINDOW_DAYS + 1L);
    }
}
