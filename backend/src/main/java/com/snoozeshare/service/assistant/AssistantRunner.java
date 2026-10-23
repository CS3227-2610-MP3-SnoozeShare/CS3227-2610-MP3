package com.snoozeshare.service.assistant;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.snoozeshare.config.ConditionalOnDatabase;

import jakarta.annotation.PreDestroy;

/** Private bounded workers; intentionally not an Executor bean (R1/R10). */
@Component
@ConditionalOnDatabase
@ConditionalOnProperty(prefix = "snoozeshare.assistant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AssistantRunner implements AutoCloseable {
    private final Object idle = new Object();
    private final Executor executor;
    private final ThreadPoolExecutor ownedPool;
    private int outstanding;
    private boolean closed;

    /** Uses daemon workers and aborts submission when the bounded queue is full. */
    @Autowired
    public AssistantRunner(AssistantProperties properties) {
        AtomicInteger sequence = new AtomicInteger();
        ownedPool = new ThreadPoolExecutor(properties.threads(), properties.threads(), 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.queue()), command -> {
                    Thread thread = new Thread(command, "assistant-" + sequence.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        executor = ownedPool;
    }

    /** Test seam: callers supply an asynchronous or rejecting executor and own its lifecycle. */
    public AssistantRunner(Executor executor) {
        this.executor = Objects.requireNonNull(executor);
        ownedPool = null;
    }

    /** Includes nested follow-up submissions in the same outstanding-work count. */
    public void submit(Runnable command) {
        Objects.requireNonNull(command);
        synchronized (idle) {
            if (closed) {
                throw new RejectedExecutionException("Assistant runner is closed");
            }
            outstanding++;
        }
        TrackedTask task = new TrackedTask(command);
        try {
            executor.execute(task);
        } catch (RuntimeException exception) {
            task.finish();
            throw exception;
        }
    }

    /** Waits using a monotonic deadline; an interrupted caller keeps its interrupt flag. */
    public boolean awaitIdle(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        synchronized (idle) {
            while (outstanding != 0) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(idle, remaining);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }
    }

    /** Interrupts owned workers and accounts for queued tasks that will never run. */
    @Override
    @PreDestroy
    public void close() {
        synchronized (idle) {
            closed = true;
        }
        if (ownedPool != null) {
            for (Runnable queued : ownedPool.shutdownNow()) {
                TrackedTask tracked = (TrackedTask) queued;
                tracked.finish();
            }
        }
    }

    private final class TrackedTask implements Runnable {
        private final Runnable command;
        private final AtomicBoolean finished = new AtomicBoolean();

        private TrackedTask(Runnable command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                command.run();
            } finally {
                finish();
            }
        }

        private void finish() {
            if (finished.compareAndSet(false, true)) {
                synchronized (idle) {
                    outstanding--;
                    if (outstanding == 0) {
                        idle.notifyAll();
                    }
                }
            }
        }
    }
}
