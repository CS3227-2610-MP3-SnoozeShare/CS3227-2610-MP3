package com.snoozeshare.domain.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class StayDatePolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);
    private final StayDatePolicy policy =
            new StayDatePolicy(Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    @Test
    void acceptsTodayAsStartAndAFutureEnd() {
        assertDoesNotThrow(() -> policy.requireValidStay(TODAY, TODAY.plusDays(2)));
    }

    @Test
    void rejectsAStartBeforeToday() {
        assertThrows(IllegalArgumentException.class, () ->
                policy.requireValidStay(TODAY.minusDays(1), TODAY.plusDays(2)));
    }

    @Test
    void rejectsEndNotAfterStartAndNulls() {
        assertThrows(IllegalArgumentException.class, () ->
                policy.requireValidStay(TODAY.plusDays(3), TODAY.plusDays(3)));
        assertThrows(IllegalArgumentException.class, () ->
                policy.requireValidStay(TODAY.plusDays(3), TODAY.plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> policy.requireValidStay(null, TODAY.plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> policy.requireValidStay(TODAY, null));
    }

    @Test
    void searchRangeRejectsInversionButAllowsOpenOrPartialRanges() {
        assertThrows(IllegalArgumentException.class, () ->
                policy.requireValidSearchRange(TODAY.plusDays(5), TODAY.plusDays(2)));
        assertDoesNotThrow(() -> policy.requireValidSearchRange(null, null));
        assertDoesNotThrow(() -> policy.requireValidSearchRange(TODAY.plusDays(1), null));
        assertDoesNotThrow(() -> policy.requireValidSearchRange(TODAY.plusDays(1), TODAY.plusDays(4)));
    }

    @Test
    void searchRangeRejectsPastDatesButAcceptsToday() {
        assertThrows(IllegalArgumentException.class, () ->
                policy.requireValidSearchRange(TODAY.minusDays(1), TODAY.plusDays(2)));
        assertThrows(IllegalArgumentException.class, () ->
                policy.requireValidSearchRange(null, TODAY.minusDays(1)));
        assertThrows(IllegalArgumentException.class, () ->
                policy.requireValidSearchRange(TODAY.minusDays(3), TODAY.minusDays(1)));
        assertDoesNotThrow(() -> policy.requireValidSearchRange(TODAY, TODAY.plusDays(1)));
    }

    @Test
    void requireNotInPastRejectsYesterdayAndAcceptsTodayAndNull() {
        assertThrows(IllegalArgumentException.class, () ->
                policy.requireNotInPast(TODAY.minusDays(1), "From date"));
        assertDoesNotThrow(() -> policy.requireNotInPast(TODAY, "From date"));
        assertDoesNotThrow(() -> policy.requireNotInPast(null, "From date"));
    }
}
