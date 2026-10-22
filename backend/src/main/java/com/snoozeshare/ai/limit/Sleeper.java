package com.snoozeshare.ai.limit;

/** Seam for waiting between retry attempts, so tests need no real sleep. */
@FunctionalInterface
public interface Sleeper {

    /** The production sleeper: blocks the calling thread. */
    Sleeper SYSTEM = Thread::sleep;

    /**
     * Blocks for the given time.
     *
     * @param millis how long to wait, in milliseconds
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void sleep(long millis) throws InterruptedException;
}
