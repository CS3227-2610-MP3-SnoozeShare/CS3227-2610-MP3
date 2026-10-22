package com.snoozeshare.ai.limit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.snoozeshare.ai.limit.CircuitBreaker.Admission;

/** E20 and E57: one streak, three users to open, a single half-open probe. */
class CircuitBreakerTest {

    private static final Instant T0 = Instant.parse("2026-10-22T10:00:00Z");

    /** A settable clock. */
    private static final class TestClock extends Clock {
        private Instant now = T0;

        void advanceMs(long ms) {
            now = now.plusMillis(ms);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final TestClock clock = new TestClock();
    private final CircuitBreaker breaker = new CircuitBreaker(clock, 5, 30_000);
    private final UUID u1 = UUID.randomUUID();
    private final UUID u2 = UUID.randomUUID();
    private final UUID u3 = UUID.randomUUID();

    @Test
    void oneUserNeverOpens() {
        for (int i = 0; i < 20; i++) {
            breaker.recordFailure(u1, false);
        }
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.ALLOWED);
    }

    @Test
    void twoUsersKeepCountingAndAThirdOpensAtOnce() {
        for (int i = 0; i < 5; i++) {
            breaker.recordFailure(i % 2 == 0 ? u1 : u2, false);
        }
        breaker.recordFailure(u1, false);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.ALLOWED);
        breaker.recordFailure(u3, false);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.REJECTED);
    }

    @Test
    void probes() {
        open();
        clock.advanceMs(29_999);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.REJECTED);
        clock.advanceMs(1);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.PROBE);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.REJECTED);
        breaker.recordResponse(true);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.ALLOWED);
        assertThat(breaker.isOpen()).isFalse();
    }

    @Test
    void probeFailureReopensForAnotherWindow() {
        open();
        clock.advanceMs(30_000);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.PROBE);
        breaker.recordFailure(u1, true);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.REJECTED);
        clock.advanceMs(29_999);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.REJECTED);
        clock.advanceMs(1);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.PROBE);
    }

    @Test
    void abandonedProbeIsGivenBack() {
        open();
        clock.advanceMs(30_000);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.PROBE);
        breaker.abandonProbe();
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.PROBE);
    }

    @Test
    void responseBelow500ResetsCountAndUsers() {
        for (int i = 0; i < 4; i++) {
            breaker.recordFailure(i % 2 == 0 ? u1 : u2, false);
        }
        breaker.recordResponse(false);
        for (int i = 0; i < 4; i++) {
            breaker.recordFailure(u3, false);
        }
        // 4 failures after the reset: below 5, and only one user
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.ALLOWED);
        breaker.recordFailure(u1, false);
        breaker.recordFailure(u2, false);
        // 6 failures, users {u3, u1, u2}
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.REJECTED);
    }

    @Test
    void stragglerResponseDoesNotCloseAnOpenBreaker() {
        open();
        breaker.recordResponse(false);
        assertThat(breaker.isOpen()).isTrue();
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.REJECTED);
    }

    private void open() {
        breaker.recordFailure(u1, false);
        breaker.recordFailure(u2, false);
        breaker.recordFailure(u3, false);
        breaker.recordFailure(u1, false);
        breaker.recordFailure(u2, false);
        assertThat(breaker.tryAdmit()).isEqualTo(Admission.REJECTED);
    }
}
