package com.snoozeshare.domain.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.model.Booking;

class DisputeWindowPolicyTest {

    private static final LocalDate END = LocalDate.of(2026, 9, 5);

    private static DisputeWindowPolicy on(LocalDate today) {
        return new DisputeWindowPolicy(Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private static Booking booking(BookingStatus status) {
        return new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), END.minusDays(4), END, status,
                new BigDecimal("100.00"), new BigDecimal("400.00"), Instant.parse("2026-08-20T00:00:00Z"),
                Instant.parse("2026-08-21T00:00:00Z"), null);
    }

    @Test
    void filingIsOpenThroughSevenDaysAfterCheckoutAndClosedOnTheEighth() {
        Booking confirmed = booking(BookingStatus.CONFIRMED);
        assertTrue(on(END.plusDays(7)).canFileTicket(confirmed));
        assertFalse(on(END.plusDays(8)).canFileTicket(confirmed));
    }

    @Test
    void filingNeedsTheTripToHaveStarted() {
        Booking confirmed = booking(BookingStatus.CONFIRMED);
        assertFalse(on(END.minusDays(5)).canFileTicket(confirmed));
        assertTrue(on(END.minusDays(4)).canFileTicket(confirmed));
    }

    @Test
    void filingNeedsConfirmedOrCompletedStatus() {
        assertTrue(on(END).canFileTicket(booking(BookingStatus.COMPLETED)));
        assertFalse(on(END).canFileTicket(booking(BookingStatus.PENDING)));
        assertFalse(on(END).canFileTicket(booking(BookingStatus.CANCELLED_BY_GUEST)));
    }

    @Test
    void autoCompleteOnlyAfterTheFilingWindowCloses() {
        Booking confirmed = booking(BookingStatus.CONFIRMED);
        assertFalse(on(END.plusDays(7)).canAutoComplete(confirmed));
        assertTrue(on(END.plusDays(8)).canAutoComplete(confirmed));
        assertFalse(on(END.plusDays(8)).canAutoComplete(booking(BookingStatus.COMPLETED)));
    }

    @Test
    void completionCutoffIsTheLastCheckoutDateTheSweepMayComplete() {
        assertEquals(END, on(END.plusDays(8)).completionCutoff());
    }

    @Test
    void neverBothFilableAndAutoCompletableOnAnyDay() {
        Booking confirmed = booking(BookingStatus.CONFIRMED);
        for (int offset = -15; offset <= 15; offset++) {
            DisputeWindowPolicy policy = on(END.plusDays(offset));
            assertFalse(policy.canFileTicket(confirmed) && policy.canAutoComplete(confirmed),
                    "both true at END+" + offset);
        }
    }
}
