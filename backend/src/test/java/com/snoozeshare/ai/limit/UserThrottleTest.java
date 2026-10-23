package com.snoozeshare.ai.limit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** E12 (rolling window) and E53 (in-flight limit). */
class UserThrottleTest {

    private static final Instant T0 = Instant.parse("2026-10-22T10:00:00Z");

    /** A settable clock. */
    private static final class TestClock extends Clock {
        private volatile Instant now = T0;

        void set(Instant instant) {
            now = instant;
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

    @Test
    void rollingWindow() {
        TestClock clock = new TestClock();
        UserThrottle throttle = new UserThrottle(clock, 2);
        UUID user = UUID.randomUUID();
        for (int i = 0; i < 6; i++) {
            assertThat(throttle.tryReserveMinute(user, "guest.chat", 6)).isTrue();
        }
        clock.set(T0.plusSeconds(10));
        for (int i = 0; i < 3; i++) {
            assertThat(throttle.tryReserveMinute(user, "guest.chat", 6)).isFalse();
        }
        clock.set(T0.plusMillis(59_999));
        assertThat(throttle.tryReserveMinute(user, "guest.chat", 6)).isFalse();
        clock.set(T0.plusSeconds(60));
        assertThat(throttle.tryReserveMinute(user, "guest.chat", 6)).isTrue();
        // the refused calls consumed nothing: five more fit, the next does not
        for (int i = 0; i < 5; i++) {
            assertThat(throttle.tryReserveMinute(user, "guest.chat", 6)).isTrue();
        }
        assertThat(throttle.tryReserveMinute(user, "guest.chat", 6)).isFalse();
    }

    @Test
    void windowIsPerUserAndFeature() {
        UserThrottle throttle = new UserThrottle(new TestClock(), 2);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertThat(throttle.tryReserveMinute(a, "guest.chat", 1)).isTrue();
        assertThat(throttle.tryReserveMinute(a, "guest.chat", 1)).isFalse();
        assertThat(throttle.tryReserveMinute(a, "host.draft", 1)).isTrue();
        assertThat(throttle.tryReserveMinute(b, "guest.chat", 1)).isTrue();
    }

    @Test
    void inFlight() throws Exception {
        UserThrottle throttle = new UserThrottle(new TestClock(), 2);
        UUID user = UUID.randomUUID();
        AtomicInteger admitted = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(5);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    if (throttle.tryEnter(user)) {
                        admitted.incrementAndGet();
                        try {
                            finish.await();
                            throw new IllegalStateException("boom");
                        } finally {
                            throttle.leave(user);
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (admitted.get() < 2 && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            Thread.sleep(100);
            assertThat(admitted.get()).isEqualTo(2);
            assertThat(throttle.inFlight(user)).isEqualTo(2);
            finish.countDown();
            int failed = 0;
            for (Future<Object> f : futures) {
                try {
                    f.get(5, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
                    failed++;
                }
            }
            assertThat(failed).isEqualTo(2);
            assertThat(throttle.inFlight(user)).isZero();
            assertThat(throttle.tryEnter(user)).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }
}
