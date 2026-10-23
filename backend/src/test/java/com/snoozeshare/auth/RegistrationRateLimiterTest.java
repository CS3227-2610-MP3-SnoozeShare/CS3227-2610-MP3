package com.snoozeshare.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.snoozeshare.auth.service.RegistrationRateLimiter;

class RegistrationRateLimiterTest {

    @Test
    void limitsUidAndIpAndResetClearsBothCounters() {
        RegistrationRateLimiter limiter = new RegistrationRateLimiter(
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(limiter.allow("uid", "ip")).isTrue();
            limiter.recordInvalid("uid", "ip");
        }
        assertThat(limiter.allow("uid", "ip")).isFalse();
        limiter.reset("uid", "ip");
        assertThat(limiter.allow("uid", "ip")).isTrue();
    }

    @Test
    void expiresCountersAfterTenMinutes() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(start);
        RegistrationRateLimiter limiter = new RegistrationRateLimiter(clock);
        for (int attempt = 0; attempt < 5; attempt++) {
            limiter.recordInvalid("uid", "ip");
        }
        assertThat(limiter.allow("uid", "ip")).isFalse();
        clock.advance(Duration.ofMinutes(10).plusNanos(1));
        assertThat(limiter.allow("uid", "ip")).isTrue();
    }

    @Test
    void resettingOneUserDoesNotResetTheSharedIpCounter() {
        RegistrationRateLimiter limiter = new RegistrationRateLimiter(
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        for (int attempt = 0; attempt < 20; attempt++) {
            limiter.recordInvalid("uid", "ip");
        }
        limiter.resetUser("uid", "ip");

        assertThat(limiter.allow("uid", "ip")).isFalse();
        assertThat(limiter.allow("other-uid", "ip")).isFalse();
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        MutableClock(Instant current) {
            this.current = current;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

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
            return current;
        }
    }
}
