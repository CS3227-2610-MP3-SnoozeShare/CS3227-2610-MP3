package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.snoozeshare.auth.api.RegistrationCodeValidationRequest;
import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.service.AuthRegistrationService.AuthRegistrationException;
import com.snoozeshare.auth.service.PublicRegistrationRateLimiter;
import com.snoozeshare.auth.service.RegistrationCodeValidationService;

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

    @Test
    void codeRotationInvalidatesAnEarlierPreflight() {
        PublicRegistrationRateLimiter limiter = new PublicRegistrationRateLimiter(Clock.systemUTC());
        RegistrationCodeValidationService oldService = new RegistrationCodeValidationService(
            limiter, "old-host", "agent-code");
        RegistrationCodeValidationService newService = new RegistrationCodeValidationService(
            limiter, "new-host", "agent-code");
        oldService.validate(new RegistrationCodeValidationRequest(AuthRole.HOST, "old-host"), "127.0.0.1");

        Executable rotatedValidation = () -> validateRotatedCode(newService);
        org.junit.jupiter.api.Assertions.assertThrows(AuthRegistrationException.class, rotatedValidation);
    }

    private void validateRotatedCode(RegistrationCodeValidationService service) {
        service.validate(new RegistrationCodeValidationRequest(AuthRole.HOST, "old-host"), "127.0.0.1");
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
