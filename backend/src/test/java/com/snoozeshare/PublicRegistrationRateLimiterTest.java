package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.service.PublicRegistrationRateLimiter;

/** Concurrency coverage for public registration-code abuse counters. */
class PublicRegistrationRateLimiterTest {

    @Test
    void concurrentInvalidAttemptsAdmitOnlyTheRoleLimit() throws Exception {
        PublicRegistrationRateLimiter limiter = new PublicRegistrationRateLimiter(Clock.systemUTC());
        ExecutorService pool = Executors.newFixedThreadPool(20);
        try {
            var futures = IntStream.range(0, 20)
                .mapToObj(index -> pool.submit(() -> limiter.recordInvalid("127.0.0.1", AuthRole.HOST)))
                .toList();
            long admitted = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    admitted++;
                }
            }
            assertThat(admitted).isEqualTo(5);
        } finally {
            pool.shutdownNow();
        }
    }
}
