package com.snoozeshare.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.snoozeshare.auth.model.AuthRole;

/** Brute-force protection for unauthenticated privileged-code pre-validation. */
@Component
public class PublicRegistrationRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final int ROLE_LIMIT = 5;
    private static final int IP_LIMIT = 20;

    private final Clock clock;
    private final Map<String, Counter> roleCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> ipCounters = new ConcurrentHashMap<>();

    public PublicRegistrationRateLimiter() {
        this(Clock.systemUTC());
    }

    public PublicRegistrationRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean recordInvalid(String ip, AuthRole role) {
        Instant now = clock.instant();
        removeExpired(now);
        String roleKey = ip + "|" + role;
        int roleCount = increment(roleCounters, roleKey, now);
        int ipCount = increment(ipCounters, ip, now);
        return roleCount <= ROLE_LIMIT && ipCount <= IP_LIMIT;
    }

    public synchronized boolean allow(String ip, AuthRole role) {
        Instant now = clock.instant();
        removeExpired(now);
        return count(roleCounters, ip + "|" + role, now) < ROLE_LIMIT
            && count(ipCounters, ip, now) < IP_LIMIT;
    }

    public synchronized void resetRole(String ip, AuthRole role) {
        roleCounters.remove(ip + "|" + role);
    }

    private void removeExpired(Instant now) {
        roleCounters.entrySet().removeIf(entry -> entry.getValue().started.plus(WINDOW).isBefore(now));
        ipCounters.entrySet().removeIf(entry -> entry.getValue().started.plus(WINDOW).isBefore(now));
    }

    private int increment(Map<String, Counter> counters, String key, Instant now) {
        Counter previous = counters.get(key);
        if (previous == null || previous.started.plus(WINDOW).isBefore(now)) {
            counters.put(key, new Counter(now, 1));
            return 1;
        }
        int count = previous.count + 1;
        counters.put(key, new Counter(previous.started, count));
        return count;
    }

    private int count(Map<String, Counter> counters, String key, Instant now) {
        Counter counter = counters.get(key);
        if (counter == null || counter.started.plus(WINDOW).isBefore(now)) {
            counters.remove(key);
            return 0;
        }
        return counter.count;
    }

    private record Counter(Instant started, int count) { }
}
