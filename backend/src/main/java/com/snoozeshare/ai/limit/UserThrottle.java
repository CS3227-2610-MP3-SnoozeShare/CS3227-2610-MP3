package com.snoozeshare.ai.limit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-user limits: a rolling 60 second window of call starts per user and feature, and a count of calls in
 * flight per user. A start at t is inside the window until t plus 60 s; a refused call consumes nothing.
 */
public final class UserThrottle {

    private static final Duration WINDOW = Duration.ofSeconds(60);
    private static final int SWEEP_SIZE = 1_024;

    private final Clock clock;
    private final int maxInFlight;
    private final Map<String, Deque<Instant>> windows = new HashMap<>();
    private final Map<UUID, Integer> inFlight = new HashMap<>();

    /**
     * Creates a throttle.
     *
     * @param clock the time source
     * @param maxInFlight calls one user may have in flight (property {@code per-user-max-in-flight})
     */
    public UserThrottle(Clock clock, int maxInFlight) {
        this.clock = clock;
        this.maxInFlight = maxInFlight;
    }

    /**
     * Reserves one slot in the user's minute window for the feature when it is under the limit. The slot stays
     * used even if a later step refuses the call.
     *
     * @param userId the user
     * @param featureId the feature id
     * @param perMinute the feature's limit per rolling 60 s
     * @return true when a slot was reserved, false when the window is full (nothing is consumed)
     */
    public synchronized boolean tryReserveMinute(UUID userId, String featureId, int perMinute) {
        Instant now = clock.instant();
        if (windows.size() > SWEEP_SIZE) {
            Instant stale = now.minus(WINDOW);
            windows.values().removeIf(d -> d.isEmpty() || !d.peekLast().isAfter(stale));
        }
        String key = userId + "|" + featureId;
        Deque<Instant> starts = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        Instant edge = now.minus(WINDOW);
        while (!starts.isEmpty() && !starts.peekFirst().isAfter(edge)) {
            starts.pollFirst();
        }
        if (starts.size() >= perMinute) {
            return false;
        }
        starts.addLast(now);
        return true;
    }

    /**
     * Takes an in-flight slot for the user when under the limit. Pair a true result with {@link #leave(UUID)}.
     *
     * @param userId the user
     * @return true when a slot was taken
     */
    public synchronized boolean tryEnter(UUID userId) {
        int current = inFlight.getOrDefault(userId, 0);
        if (current >= maxInFlight) {
            return false;
        }
        inFlight.put(userId, current + 1);
        return true;
    }

    /**
     * Releases an in-flight slot taken by {@link #tryEnter(UUID)}.
     *
     * @param userId the user
     */
    public synchronized void leave(UUID userId) {
        inFlight.computeIfPresent(userId, (id, n) -> n <= 1 ? null : n - 1);
    }

    /**
     * Returns the user's current in-flight count.
     *
     * @param userId the user
     * @return the count
     */
    public synchronized int inFlight(UUID userId) {
        return inFlight.getOrDefault(userId, 0);
    }
}
