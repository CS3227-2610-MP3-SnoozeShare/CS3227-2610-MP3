package com.snoozeshare.ai.limit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.snoozeshare.ai.config.LlmProperties;

/** Blocking wrapper over {@link SchedulerCore}: a lock and a condition that wake waiting threads. */
public final class LlmScheduler {

    /** Why {@link #acquire} refused. */
    public enum Reason {
        /** {@code max-queue} jobs were already waiting, or the job was displaced by a priority 0 job. */
        QUEUE_FULL,
        /** The wait ran out. */
        QUEUE_TIMEOUT,
        /** The thread was interrupted; its interrupt flag is set again. */
        INTERRUPTED
    }

    /** Thrown by {@link #acquire} when no permit was granted. */
    public static final class SchedulerRejection extends Exception {
        private static final long serialVersionUID = 1L;

        private final Reason reason;

        SchedulerRejection(Reason reason) {
            super(reason.name(), null, false, false);
            this.reason = reason;
        }

        /**
         * Returns why the permit was refused.
         *
         * @return the reason
         */
        public Reason reason() {
            return reason;
        }
    }

    /** A granted slot; closing it twice is harmless. */
    public final class Permit implements AutoCloseable {
        private final SchedulerCore.Job job;
        private final AtomicBoolean released = new AtomicBoolean();

        private Permit(SchedulerCore.Job job) {
            this.job = job;
        }

        /** Returns the slot and wakes waiting threads. */
        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                lock.lock();
                try {
                    core.release(job);
                    changed.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    private final Clock clock;
    private final SchedulerCore core;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition changed = lock.newCondition();

    /**
     * Creates a scheduler from the configured limits.
     *
     * @param props the gateway settings
     * @param clock the time source
     */
    public LlmScheduler(LlmProperties props, Clock clock) {
        this(new SchedulerCore(props.maxRpm(), props.maxConcurrent(), props.maxQueue()), clock);
    }

    LlmScheduler(SchedulerCore core, Clock clock) {
        this.core = core;
        this.clock = clock;
    }

    /**
     * Waits for a slot.
     *
     * @param priority the feature priority, 0 (most urgent) to 2
     * @param arrival when the call's first attempt arrived; retries pass the same value
     * @param maxWait the longest to wait for the slot
     * @return the permit; close it when the attempt ends
     * @throws SchedulerRejection when the queue is full, the wait ran out or the thread was interrupted
     */
    public Permit acquire(int priority, Instant arrival, Duration maxWait) throws SchedulerRejection {
        lock.lock();
        SchedulerCore.Job job = null;
        try {
            Instant now = clock.instant();
            Instant giveUp = now.plus(maxWait);
            SchedulerCore.Offered offered = core.offer(priority, arrival, now);
            changed.signalAll();
            job = offered.job();
            if (job == null) {
                throw new SchedulerRejection(Reason.QUEUE_FULL);
            }
            while (true) {
                if (job.status() == SchedulerCore.Status.RUNNING) {
                    return new Permit(job);
                }
                if (job.status() == SchedulerCore.Status.DISPLACED) {
                    throw new SchedulerRejection(Reason.QUEUE_FULL);
                }
                now = clock.instant();
                if (!core.promote(now).isEmpty()) {
                    changed.signalAll();
                    continue;
                }
                Duration left = Duration.between(now, giveUp);
                if (left.isNegative() || left.isZero()) {
                    core.cancel(job);
                    changed.signalAll();
                    throw new SchedulerRejection(Reason.QUEUE_TIMEOUT);
                }
                Instant reopens = core.nextWindowOpening(now);
                Duration pause = reopens == null ? left : min(left, Duration.between(now, reopens));
                changed.awaitNanos(Math.max(TimeUnit.MILLISECONDS.toNanos(1), pause.toNanos()));
            }
        } catch (InterruptedException e) {
            abandon(job);
            Thread.currentThread().interrupt();
            throw new SchedulerRejection(Reason.INTERRUPTED);
        } finally {
            lock.unlock();
        }
    }

    private void abandon(SchedulerCore.Job job) {
        if (job != null && !core.cancel(job)) {
            core.release(job);
        }
        changed.signalAll();
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
