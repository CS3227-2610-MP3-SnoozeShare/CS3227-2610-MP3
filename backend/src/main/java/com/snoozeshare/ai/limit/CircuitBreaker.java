package com.snoozeshare.ai.limit;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Circuit breaker over SoCLaaS failures. One streak holds the failed-attempt count and the users that caused
 * them; it opens for {@code breaker-open-ms} when the count reaches {@code breaker-failures} and at least three
 * distinct users are in the set. After the window exactly one probe call runs: success closes the breaker,
 * failure re-opens it for another window. Any response below 500 resets the streak.
 */
public final class CircuitBreaker {

    /** What the breaker or budget block decides for a call at the start of an attempt. */
    public enum Admission {
        /** Proceed normally. */
        ALLOWED,
        /** Proceed as the single probe: one attempt, no retries, and report the result. */
        PROBE,
        /** Refuse the call. */
        REJECTED
    }

    private static final int MIN_USERS = 3;

    private final Clock clock;
    private final int failuresToOpen;
    private final long openMs;
    private final Set<UUID> streakUsers = new HashSet<>();
    private int streakCount;
    private boolean open;
    private Instant reopenAt = Instant.MIN;
    private boolean probeInFlight;

    /**
     * Creates a closed breaker.
     *
     * @param clock the time source
     * @param failuresToOpen failed attempts that open it ({@code breaker-failures})
     * @param openMs how long it stays open ({@code breaker-open-ms})
     */
    public CircuitBreaker(Clock clock, int failuresToOpen, long openMs) {
        this.clock = clock;
        this.failuresToOpen = failuresToOpen;
        this.openMs = openMs;
    }

    /**
     * Decides whether an attempt may go. Open and still inside the window, or open with the probe in flight,
     * is rejected; open past the window hands out the single probe.
     *
     * @return the decision
     */
    public synchronized Admission tryAdmit() {
        if (!open) {
            return Admission.ALLOWED;
        }
        if (probeInFlight || clock.instant().isBefore(reopenAt)) {
            return Admission.REJECTED;
        }
        probeInFlight = true;
        return Admission.PROBE;
    }

    /**
     * Records a failed attempt (SERVER, TIMEOUT or NETWORK).
     *
     * @param userId the user whose call failed
     * @param probe whether this attempt was the probe
     */
    public synchronized void recordFailure(UUID userId, boolean probe) {
        if (probe) {
            probeInFlight = false;
            openWindow();
            return;
        }
        if (open) {
            return;
        }
        streakCount++;
        streakUsers.add(userId);
        if (streakCount >= failuresToOpen && streakUsers.size() >= MIN_USERS) {
            openWindow();
        }
    }

    /**
     * Records an attempt that received an HTTP response below 500: the streak resets, and a probe closes the
     * breaker. A straggler from before the breaker opened does not close it.
     *
     * @param probe whether this attempt was the probe
     */
    public synchronized void recordResponse(boolean probe) {
        if (probe) {
            probeInFlight = false;
            open = false;
        }
        if (!open) {
            streakCount = 0;
            streakUsers.clear();
        }
    }

    /** Gives the probe back when its call ended without an outcome (interrupt, deadline before sending). */
    public synchronized void abandonProbe() {
        probeInFlight = false;
    }

    /**
     * Whether the breaker is open or half-open.
     *
     * @return true when not closed
     */
    public synchronized boolean isOpen() {
        return open;
    }

    private void openWindow() {
        open = true;
        reopenAt = clock.instant().plusMillis(openMs);
        streakCount = 0;
        streakUsers.clear();
    }
}
