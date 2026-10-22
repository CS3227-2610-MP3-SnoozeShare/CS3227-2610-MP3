package com.snoozeshare.ai.limit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Pure scheduler state machine, not thread-safe: the caller supplies the instant and holds a lock. It enforces
 * at most {@code max-rpm} starts in any rolling window of (t - 60 s, t], at most {@code max-concurrent} running
 * jobs, a queue of at most {@code max-queue} waiting jobs, priority 0 first, and a one-slot reserve for
 * priority 0 (priority 1 and 2 behave alike and may use at most {@code max-concurrent - 1} slots).
 */
public final class SchedulerCore {

    private static final Duration WINDOW = Duration.ofSeconds(60);

    /** Where a job is in its life. */
    public enum Status {
        /** Waiting in the queue. */
        QUEUED,
        /** Started and holding a slot until released. */
        RUNNING,
        /** Removed from a full queue to make room for a priority 0 job. */
        DISPLACED,
        /** Left the queue without starting (timeout or interrupt). */
        CANCELLED,
        /** Finished and released. */
        DONE
    }

    /** One request for a slot; created by {@link #offer}. */
    public static final class Job {
        private final int priority;
        private final Instant arrival;
        private final long seq;
        private Status status = Status.QUEUED;

        private Job(int priority, Instant arrival, long seq) {
            this.priority = priority;
            this.arrival = arrival;
            this.seq = seq;
        }

        /**
         * Returns the feature priority, 0 (most urgent) to 2.
         *
         * @return the priority
         */
        public int priority() {
            return priority;
        }

        /**
         * Returns the arrival instant of the call's first attempt, kept across retries.
         *
         * @return the arrival
         */
        public Instant arrival() {
            return arrival;
        }

        /**
         * Returns the job's current status.
         *
         * @return the status
         */
        public Status status() {
            return status;
        }
    }

    /**
     * Outcome of {@link #offer}.
     *
     * @param job the queued (or already started) job, or null when the queue was full (QUEUE_FULL)
     * @param displaced the waiting job that was removed to make room, or null
     */
    public record Offered(Job job, Job displaced) { }

    private static final Comparator<Job> ORDER = Comparator
        .comparingInt((Job j) -> j.priority == 0 ? 0 : 1)
        .thenComparing(j -> j.arrival)
        .thenComparingLong(j -> j.seq);

    private final int maxRpm;
    private final int maxConcurrent;
    private final int maxQueue;
    private final PriorityQueue<Job> queue = new PriorityQueue<>(ORDER);
    private final Deque<Instant> starts = new ArrayDeque<>();
    private int running;
    private long nextSeq;

    /**
     * Creates an empty scheduler.
     *
     * @param maxRpm starts allowed per rolling minute
     * @param maxConcurrent jobs that may run at once
     * @param maxQueue jobs that may wait
     */
    public SchedulerCore(int maxRpm, int maxConcurrent, int maxQueue) {
        this.maxRpm = maxRpm;
        this.maxConcurrent = maxConcurrent;
        this.maxQueue = maxQueue;
    }

    /**
     * Asks for a slot. Jobs that can start do so first; if the queue is then full, a priority 0 job displaces
     * the youngest waiting job of priority 1 or 2, and anything else is refused.
     *
     * @param priority the feature priority, 0 to 2
     * @param arrival when the call's first attempt arrived (retries pass the original value)
     * @param now the current instant
     * @return the job (possibly already RUNNING) and any displaced job; job null means the queue is full
     */
    public Offered offer(int priority, Instant arrival, Instant now) {
        promote(now);
        Job displaced = null;
        if (queue.size() >= maxQueue) {
            displaced = priority == 0 ? youngestNonUrgent() : null;
            if (displaced == null) {
                return new Offered(null, null);
            }
            queue.remove(displaced);
            displaced.status = Status.DISPLACED;
        }
        Job job = new Job(priority, arrival, nextSeq++);
        queue.add(job);
        promote(now);
        return new Offered(job, displaced);
    }

    /**
     * Starts as many waiting jobs as the limits allow, best first.
     *
     * @param now the current instant
     * @return the jobs that started
     */
    public List<Job> promote(Instant now) {
        List<Job> started = new ArrayList<>();
        while (!queue.isEmpty() && canStart(queue.peek(), now)) {
            Job job = queue.poll();
            job.status = Status.RUNNING;
            running++;
            starts.addLast(now);
            started.add(job);
        }
        return started;
    }

    /**
     * Returns a slot held by a running job.
     *
     * @param job a job in status RUNNING; any other job is ignored
     */
    public void release(Job job) {
        if (job.status == Status.RUNNING) {
            job.status = Status.DONE;
            running--;
        }
    }

    /**
     * Removes a waiting job that gave up.
     *
     * @param job the job
     * @return true when it was waiting and is now CANCELLED; false when it had already started or ended
     */
    public boolean cancel(Job job) {
        if (job.status == Status.QUEUED && queue.remove(job)) {
            job.status = Status.CANCELLED;
            return true;
        }
        return false;
    }

    /**
     * Returns when the rate window next frees a start, for a caller that must wait for it.
     *
     * @param now the current instant
     * @return the instant the oldest counted start leaves the window, or null when the rate is not limiting
     */
    public Instant nextWindowOpening(Instant now) {
        prune(now);
        return starts.size() >= maxRpm ? starts.peekFirst().plus(WINDOW) : null;
    }

    /**
     * Returns the number of running jobs.
     *
     * @return the count
     */
    public int running() {
        return running;
    }

    /**
     * Returns the number of waiting jobs.
     *
     * @return the count
     */
    public int queued() {
        return queue.size();
    }

    private boolean canStart(Job job, Instant now) {
        prune(now);
        int cap = job.priority == 0 ? maxConcurrent : maxConcurrent - 1;
        return running < cap && starts.size() < maxRpm;
    }

    private void prune(Instant now) {
        Instant edge = now.minus(WINDOW);
        while (!starts.isEmpty() && !starts.peekFirst().isAfter(edge)) {
            starts.pollFirst();
        }
    }

    private Job youngestNonUrgent() {
        Job youngest = null;
        for (Job j : queue) {
            if (j.priority >= 1 && (youngest == null || ORDER.compare(j, youngest) > 0)) {
                youngest = j;
            }
        }
        return youngest;
    }
}
