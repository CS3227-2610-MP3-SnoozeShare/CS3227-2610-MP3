package com.snoozeshare.ai.limit;

/** Delay between retry attempts: {@code min(8000, 500 x 2^n)} times a jitter factor, or {@code Retry-After}. */
public final class Backoff {

    private static final long BASE_MS = 500;
    private static final long MAX_MS = 8_000;
    private static final long RETRY_AFTER_CAP_MS = 10_000;
    private static final int MAX_SHIFT = 20;

    private final Jitter jitter;

    /**
     * Creates a backoff.
     *
     * @param jitter the source of the jitter factor
     */
    public Backoff(Jitter jitter) {
        this.jitter = jitter;
    }

    /**
     * Returns how long to sleep before retry number {@code retryIndex}.
     *
     * @param retryIndex 0 before the first retry, 1 before the second, and so on
     * @param retryAfterSeconds the upstream {@code Retry-After} in whole seconds, or null when absent
     * @return the delay in milliseconds
     */
    public long delayMs(int retryIndex, Long retryAfterSeconds) {
        if (retryAfterSeconds != null && retryAfterSeconds >= 0) {
            return Math.min(RETRY_AFTER_CAP_MS, saturatedMillis(retryAfterSeconds));
        }
        long exponential = BASE_MS << Math.max(0, Math.min(retryIndex, MAX_SHIFT));
        return Math.round(Math.min(MAX_MS, exponential) * jitter.clamped());
    }

    /**
     * Parses a {@code Retry-After} header holding whole seconds; an HTTP-date or any other value is ignored.
     *
     * @param header the header value, may be null
     * @return the seconds, or null when the header is absent or not whole seconds
     */
    public static Long parseRetryAfter(String header) {
        if (header == null) {
            return null;
        }
        String value = header.trim();
        if (value.isEmpty() || value.length() > 9 || !value.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return null;
        }
        return Long.parseLong(value);
    }

    private static long saturatedMillis(long seconds) {
        return seconds > RETRY_AFTER_CAP_MS ? RETRY_AFTER_CAP_MS : seconds * 1_000;
    }
}
