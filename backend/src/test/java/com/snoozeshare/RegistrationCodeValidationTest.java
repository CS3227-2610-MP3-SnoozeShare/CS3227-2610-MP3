package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.service.PublicRegistrationRateLimiter;

/** Unit coverage for the public pre-validation abuse counters. */
class RegistrationCodeValidationTest {

    @Test
    void invalidAttemptsAreLimitedPerRoleAndExpire() {
        TestClock clock = new TestClock();
        PublicRegistrationRateLimiter limiter = new PublicRegistrationRateLimiter(clock);

        assertThat(limiter.recordInvalid("127.0.0.1", AuthRole.HOST)).isTrue();
        assertThat(limiter.recordInvalid("127.0.0.1", AuthRole.HOST)).isTrue();
        assertThat(limiter.recordInvalid("127.0.0.1", AuthRole.HOST)).isTrue();
        assertThat(limiter.recordInvalid("127.0.0.1", AuthRole.HOST)).isTrue();
        assertThat(limiter.recordInvalid("127.0.0.1", AuthRole.HOST)).isTrue();
        assertThat(limiter.recordInvalid("127.0.0.1", AuthRole.HOST)).isFalse();
        assertThat(limiter.recordInvalid("127.0.0.1", AuthRole.AGENT)).isTrue();

        clock.advanceSeconds(601);
        assertThat(limiter.recordInvalid("127.0.0.1", AuthRole.HOST)).isTrue();
    }

    @Test
    void validCodeResetsOnlyTheSelectedRole() {
        PublicRegistrationRateLimiter limiter = new PublicRegistrationRateLimiter(Clock.systemUTC());
        for (int i = 0; i < 5; i++) {
            limiter.recordInvalid("127.0.0.1", AuthRole.HOST);
            limiter.recordInvalid("127.0.0.1", AuthRole.AGENT);
        }

        limiter.resetRole("127.0.0.1", AuthRole.HOST);

        assertThat(limiter.recordInvalid("127.0.0.1", AuthRole.HOST)).isTrue();
        assertThat(limiter.recordInvalid("127.0.0.1", AuthRole.AGENT)).isFalse();
    }

    private static final class TestClock extends Clock {
        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }
}
