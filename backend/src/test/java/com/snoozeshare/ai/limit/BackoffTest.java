package com.snoozeshare.ai.limit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** E18 arithmetic: exponential delay, jitter clamp, Retry-After handling. */
class BackoffTest {

    @Test
    void exponentialAtFullJitter() {
        Backoff backoff = new Backoff(() -> 1.0);
        assertThat(backoff.delayMs(0, null)).isEqualTo(500);
        assertThat(backoff.delayMs(1, null)).isEqualTo(1_000);
        assertThat(backoff.delayMs(2, null)).isEqualTo(2_000);
        assertThat(backoff.delayMs(4, null)).isEqualTo(8_000);
        assertThat(backoff.delayMs(5, null)).isEqualTo(8_000);
        assertThat(backoff.delayMs(500, null)).isEqualTo(8_000);
    }

    @Test
    void jitterIsClamped() {
        assertThat(new Backoff(() -> 0.0).delayMs(1, null)).isEqualTo(500);
        assertThat(new Backoff(() -> -3.0).delayMs(1, null)).isEqualTo(500);
        assertThat(new Backoff(() -> 7.0).delayMs(1, null)).isEqualTo(1_000);
        assertThat(new Backoff(() -> Double.NaN).delayMs(0, null)).isEqualTo(500);
        assertThat(new Backoff(() -> 0.75).delayMs(0, null)).isEqualTo(375);
    }

    @Test
    void systemJitterStaysInRange() {
        for (int i = 0; i < 1_000; i++) {
            assertThat(Jitter.SYSTEM.clamped()).isBetween(0.5, 1.0);
        }
    }

    @Test
    void retryAfterWinsAndIsCapped() {
        Backoff backoff = new Backoff(() -> 0.5);
        assertThat(backoff.delayMs(0, 30L)).isEqualTo(10_000);
        assertThat(backoff.delayMs(0, 2L)).isEqualTo(2_000);
        assertThat(backoff.delayMs(3, 0L)).isZero();
        assertThat(backoff.delayMs(0, Long.MAX_VALUE)).isEqualTo(10_000);
    }

    @Test
    void retryAfterParsing() {
        assertThat(Backoff.parseRetryAfter("30")).isEqualTo(30L);
        assertThat(Backoff.parseRetryAfter(" 2 ")).isEqualTo(2L);
        assertThat(Backoff.parseRetryAfter("Wed, 21 Oct 2026 07:28:00 GMT")).isNull();
        assertThat(Backoff.parseRetryAfter("-1")).isNull();
        assertThat(Backoff.parseRetryAfter("1.5")).isNull();
        assertThat(Backoff.parseRetryAfter("")).isNull();
        assertThat(Backoff.parseRetryAfter(null)).isNull();
        assertThat(Backoff.parseRetryAfter("99999999999999999999")).isNull();
    }
}
