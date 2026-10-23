package com.snoozeshare.ai.limit;

import java.util.concurrent.ThreadLocalRandom;

/** Seam for the random factor applied to a backoff delay. Consumers clamp it to [0.5, 1.0]. */
@FunctionalInterface
public interface Jitter {

    /** Lowest factor a backoff accepts. */
    double MIN = 0.5;
    /** Highest factor a backoff accepts. */
    double MAX = 1.0;

    /** The production jitter: uniform in [0.5, 1.0]. */
    Jitter SYSTEM = () -> MIN + ThreadLocalRandom.current().nextDouble() * (MAX - MIN);

    /**
     * Returns the next factor.
     *
     * @return a factor; values outside [0.5, 1.0] are clamped by {@link #clamped()}
     */
    double factor();

    /**
     * Returns the next factor clamped to [0.5, 1.0]; NaN counts as 1.0.
     *
     * @return a factor in [0.5, 1.0]
     */
    default double clamped() {
        double f = factor();
        if (Double.isNaN(f)) {
            return MAX;
        }
        return Math.max(MIN, Math.min(MAX, f));
    }
}
