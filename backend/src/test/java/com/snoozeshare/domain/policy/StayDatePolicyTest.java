package com.snoozeshare.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class StayDatePolicyTest {

    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");
    private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private final StayDatePolicy policy = new StayDatePolicy(FIXED);

    // --- requireValidStay ---

    @Test
    void validStayAccepted() {
        assertThatCode(() -> policy.requireValidStay(TODAY, TODAY.plusDays(3)))
                .doesNotThrowAnyException();
    }

    @Test
    void stayStartingTodayAccepted() {
        assertThatCode(() -> policy.requireValidStay(TODAY, TODAY.plusDays(1)))
                .doesNotThrowAnyException();
    }

    @Test
    void nullStartRejected() {
        assertThatThrownBy(() -> policy.requireValidStay(null, TODAY.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullEndRejected() {
        assertThatThrownBy(() -> policy.requireValidStay(TODAY, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pastStartRejected() {
        assertThatThrownBy(() -> policy.requireValidStay(TODAY.minusDays(1), TODAY.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    void endNotAfterStartRejected() {
        assertThatThrownBy(() -> policy.requireValidStay(TODAY, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after");
    }

    // --- requireValidSearchRange ---

    @Test
    void bothNullSearchDatesAccepted() {
        assertThatCode(() -> policy.requireValidSearchRange(null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void openEndedSearchAccepted() {
        assertThatCode(() -> policy.requireValidSearchRange(TODAY, null))
                .doesNotThrowAnyException();
    }

    @Test
    void pastFromRejected() {
        assertThatThrownBy(() -> policy.requireValidSearchRange(TODAY.minusDays(1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pastToRejected() {
        assertThatThrownBy(() -> policy.requireValidSearchRange(null, TODAY.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invertedRangeRejected() {
        assertThatThrownBy(() -> policy.requireValidSearchRange(TODAY.plusDays(3), TODAY.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- requireNotInPast ---

    @Test
    void futureAccepted() {
        assertThatCode(() -> policy.requireNotInPast(TODAY.plusDays(1), "Check-in"))
                .doesNotThrowAnyException();
    }

    @Test
    void todayAccepted() {
        assertThatCode(() -> policy.requireNotInPast(TODAY, "Check-in"))
                .doesNotThrowAnyException();
    }

    @Test
    void nullAccepted() {
        assertThatCode(() -> policy.requireNotInPast(null, "Check-in"))
                .doesNotThrowAnyException();
    }

    @Test
    void pastRejected() {
        assertThatThrownBy(() -> policy.requireNotInPast(TODAY.minusDays(1), "Check-in"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Check-in");
    }
}
