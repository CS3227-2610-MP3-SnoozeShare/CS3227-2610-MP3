package com.snoozeshare.ai.limit;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.snoozeshare.ai.limit.CircuitBreaker.Admission;

/**
 * Blocks all calls for {@code budget-block-ms} (capped at the next 00:00 UTC) after an upstream budget 429.
 * After the window the next call is sent as a single probe; a repeated budget 429 extends the block, any other
 * response lifts it.
 */
public final class BudgetBlock {

    private final Clock clock;
    private final long blockMs;
    private boolean blocked;
    private Instant blockedUntil = Instant.MIN;
    private boolean probeInFlight;

    /**
     * Creates an inactive block.
     *
     * @param clock the time source (any zone; day boundaries are computed in UTC)
     * @param blockMs the block length ({@code budget-block-ms})
     */
    public BudgetBlock(Clock clock, long blockMs) {
        this.clock = clock;
        this.blockMs = blockMs;
    }

    /**
     * Decides whether an attempt may go.
     *
     * @return ALLOWED when never blocked or lifted, REJECTED inside the window or while the probe is in
     *     flight, PROBE for the first call after the window
     */
    public synchronized Admission tryAdmit() {
        if (!blocked) {
            return Admission.ALLOWED;
        }
        if (probeInFlight || clock.instant().isBefore(blockedUntil)) {
            return Admission.REJECTED;
        }
        probeInFlight = true;
        return Admission.PROBE;
    }

    /** Records an upstream budget 429: (re)starts the block, capped at the next 00:00 UTC. */
    public synchronized void recordBudgetExhausted() {
        Instant now = clock.instant();
        Instant end = now.plusMillis(blockMs);
        Instant midnight = now.atZone(ZoneOffset.UTC).toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC)
            .toInstant();
        blockedUntil = end.isAfter(midnight) ? midnight : end;
        blocked = true;
        probeInFlight = false;
    }

    /**
     * Records an attempt that got a response that was not a budget 429. Only the probe lifts the block.
     *
     * @param probe whether this attempt was the probe
     */
    public synchronized void recordOtherResponse(boolean probe) {
        if (probe) {
            probeInFlight = false;
            blocked = false;
        }
    }

    /** Gives the probe back when its call ended without an outcome (interrupt, deadline before sending). */
    public synchronized void abandonProbe() {
        probeInFlight = false;
    }
}
