package com.snoozeshare.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** Process-local brute-force protection for invalid privileged registration codes. */
@Component
public class RegistrationRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final int UID_IP_LIMIT = 5;
    private static final int IP_LIMIT = 20;

    private final Clock clock;
    private final Map<String, Counter> uidIpCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> ipCounters = new ConcurrentHashMap<>();

    public RegistrationRateLimiter() {
        this(Clock.systemUTC());
    }

    public RegistrationRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean allow(String uid, String ip) {
        Instant now = clock.instant();
        return count(uidIpCounters, uid + "|" + ip, now) < UID_IP_LIMIT
            && count(ipCounters, ip, now) < IP_LIMIT;
    }

    public synchronized void recordInvalid(String uid, String ip) {
        Instant now = clock.instant();
        increment(uidIpCounters, uid + "|" + ip, now);
        increment(ipCounters, ip, now);
    }

    public synchronized void reset(String uid, String ip) {
        uidIpCounters.remove(uid + "|" + ip);
        ipCounters.remove(ip);
    }

    private int count(Map<String, Counter> counters, String key, Instant now) {
        Counter counter = counters.get(key);
        if (counter == null || counter.started.plus(WINDOW).isBefore(now)) {
            counters.remove(key);
            return 0;
        }
        return counter.count;
    }

    private void increment(Map<String, Counter> counters, String key, Instant now) {
        Counter counter = counters.get(key);
        if (counter == null || counter.started.plus(WINDOW).isBefore(now)) {
            counters.put(key, new Counter(now, 1));
        } else {
            counters.put(key, new Counter(counter.started, counter.count + 1));
        }
    }

    private record Counter(Instant started, int count) { }
}
