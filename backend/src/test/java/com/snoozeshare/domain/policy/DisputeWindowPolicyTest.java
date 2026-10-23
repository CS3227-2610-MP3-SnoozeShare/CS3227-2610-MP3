package com.snoozeshare.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.model.Booking;

class DisputeWindowPolicyTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 6, 10);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 6, 13);

    private DisputeWindowPolicy policyAt(LocalDate today) {
        Instant instant = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        return new DisputeWindowPolicy(Clock.fixed(instant, ZoneOffset.UTC));
    }

    private Booking booking(BookingStatus status) {
        return new Booking(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                CHECK_IN, CHECK_OUT, status,
                new BigDecimal("100.00"), new BigDecimal("300.00"),
                Instant.now(), null, null, null);
    }

    // --- canFileTicket ---

    @Test
    void canFileOnStartDate() {
        assertThat(policyAt(CHECK_IN).canFileTicket(booking(BookingStatus.CONFIRMED))).isTrue();
    }

    @Test
    void canFileOnCheckOutPlusSevenDays() {
        assertThat(policyAt(CHECK_OUT.plusDays(7)).canFileTicket(booking(BookingStatus.CONFIRMED)))
                .isTrue();
    }

    @Test
    void cannotFileOnCheckOutPlusEightDays() {
        assertThat(policyAt(CHECK_OUT.plusDays(8)).canFileTicket(booking(BookingStatus.CONFIRMED)))
                .isFalse();
    }

    @Test
    void cannotFileBeforeStayStarts() {
        assertThat(policyAt(CHECK_IN.minusDays(1)).canFileTicket(booking(BookingStatus.CONFIRMED)))
                .isFalse();
    }

    @Test
    void canFileForCompletedBooking() {
        assertThat(policyAt(CHECK_OUT.plusDays(1)).canFileTicket(booking(BookingStatus.COMPLETED)))
                .isTrue();
    }

    @Test
    void cannotFileForPendingBooking() {
        assertThat(policyAt(CHECK_IN).canFileTicket(booking(BookingStatus.PENDING))).isFalse();
    }

    @Test
    void cannotFileForCancelledBooking() {
        assertThat(policyAt(CHECK_IN).canFileTicket(booking(BookingStatus.CANCELLED_BY_GUEST)))
                .isFalse();
    }

    // --- canAutoComplete ---

    @Test
    void canAutoCompleteAfterWindow() {
        assertThat(policyAt(CHECK_OUT.plusDays(8))
                .canAutoComplete(booking(BookingStatus.CONFIRMED))).isTrue();
    }

    @Test
    void cannotAutoCompleteWithinWindow() {
        assertThat(policyAt(CHECK_OUT.plusDays(7))
                .canAutoComplete(booking(BookingStatus.CONFIRMED))).isFalse();
    }

    @Test
    void cannotAutoCompleteAlreadyCompleted() {
        assertThat(policyAt(CHECK_OUT.plusDays(8))
                .canAutoComplete(booking(BookingStatus.COMPLETED))).isFalse();
    }

    // --- completionCutoff ---

    @Test
    void completionCutoffIsTodayMinusEight() {
        DisputeWindowPolicy policy = policyAt(LocalDate.of(2026, 7, 1));
        assertThat(policy.completionCutoff()).isEqualTo(LocalDate.of(2026, 6, 23));
    }
}
