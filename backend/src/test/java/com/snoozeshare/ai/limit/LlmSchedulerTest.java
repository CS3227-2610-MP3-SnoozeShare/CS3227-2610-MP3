package com.snoozeshare.ai.limit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.snoozeshare.ai.limit.LlmScheduler.Permit;
import com.snoozeshare.ai.limit.LlmScheduler.Reason;
import com.snoozeshare.ai.limit.LlmScheduler.SchedulerRejection;

/** The blocking wrapper on the real clock: wake-up, timeout, displacement and the E51 interrupt. */
class LlmSchedulerTest {

    private final Clock clock = Clock.systemUTC();

    private LlmScheduler scheduler(int maxRpm, int maxConcurrent, int maxQueue) {
        return new LlmScheduler(new SchedulerCore(maxRpm, maxConcurrent, maxQueue), clock);
    }

    private Permit acquire(LlmScheduler scheduler, int priority, Duration wait) throws SchedulerRejection {
        return scheduler.acquire(priority, clock.instant(), wait);
    }

    private Reason rejectionOf(LlmScheduler scheduler, int priority, Duration wait) {
        try {
            acquire(scheduler, priority, wait).close();
            return null;
        } catch (SchedulerRejection e) {
            return e.reason();
        }
    }

    private CompletableFuture<Reason> rejectionAsync(LlmScheduler scheduler, int priority, Duration wait) {
        return CompletableFuture.supplyAsync(() -> rejectionOf(scheduler, priority, wait));
    }

    @Test
    void waitingJobWakesWhenAPermitIsReleased() throws Exception {
        LlmScheduler scheduler = scheduler(55, 2, 5);
        Permit a = acquire(scheduler, 0, Duration.ofSeconds(1));
        Permit b = acquire(scheduler, 0, Duration.ofSeconds(1));
        CompletableFuture<Reason> waiter = rejectionAsync(scheduler, 0, Duration.ofSeconds(5));
        Thread.sleep(100);
        assertThat(waiter).isNotDone();
        a.close();
        a.close();
        assertThat(waiter.get(2, TimeUnit.SECONDS)).isNull();
        b.close();
    }

    @Test
    void queueTimeout() throws Exception {
        LlmScheduler scheduler = scheduler(55, 2, 5);
        Permit a = acquire(scheduler, 0, Duration.ofSeconds(1));
        Permit b = acquire(scheduler, 0, Duration.ofSeconds(1));
        long started = System.nanoTime();
        assertThat(rejectionOf(scheduler, 0, Duration.ofMillis(150))).isEqualTo(Reason.QUEUE_TIMEOUT);
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isGreaterThanOrEqualTo(140);
        a.close();
        b.close();
        // the timed-out job left no state behind
        assertThat(rejectionOf(scheduler, 0, Duration.ofMillis(100))).isNull();
    }

    @Test
    void queueFull() throws Exception {
        LlmScheduler scheduler = scheduler(55, 2, 1);
        Permit a = acquire(scheduler, 0, Duration.ofSeconds(1));
        Permit b = acquire(scheduler, 0, Duration.ofSeconds(1));
        CompletableFuture<Reason> waiter = rejectionAsync(scheduler, 2, Duration.ofSeconds(5));
        Thread.sleep(100);
        assertThat(rejectionOf(scheduler, 2, Duration.ofSeconds(1))).isEqualTo(Reason.QUEUE_FULL);
        // priority 2 may use at most max-concurrent - 1 slots, so it starts only when both slots are free
        a.close();
        b.close();
        assertThat(waiter.get(2, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void displacedWaiterGetsQueueFull() throws Exception {
        LlmScheduler scheduler = scheduler(55, 2, 1);
        Permit a = acquire(scheduler, 0, Duration.ofSeconds(1));
        Permit b = acquire(scheduler, 0, Duration.ofSeconds(1));
        CompletableFuture<Reason> low = rejectionAsync(scheduler, 2, Duration.ofSeconds(5));
        Thread.sleep(100);
        CompletableFuture<Reason> urgent = rejectionAsync(scheduler, 0, Duration.ofSeconds(5));
        assertThat(low.get(2, TimeUnit.SECONDS)).isEqualTo(Reason.QUEUE_FULL);
        a.close();
        assertThat(urgent.get(2, TimeUnit.SECONDS)).isNull();
        b.close();
    }

    @Test
    void interrupt() throws Exception {
        LlmScheduler scheduler = scheduler(55, 2, 5);
        Permit a = acquire(scheduler, 0, Duration.ofSeconds(1));
        Permit b = acquire(scheduler, 0, Duration.ofSeconds(1));
        AtomicBoolean flagRestored = new AtomicBoolean();
        CompletableFuture<Reason> result = new CompletableFuture<>();
        Thread t = new Thread(() -> {
            Reason reason = rejectionOf(scheduler, 0, Duration.ofSeconds(10));
            flagRestored.set(Thread.currentThread().isInterrupted());
            result.complete(reason);
        });
        t.start();
        Thread.sleep(150);
        t.interrupt();
        assertThat(result.get(2, TimeUnit.SECONDS)).isEqualTo(Reason.INTERRUPTED);
        assertThat(flagRestored).isTrue();
        // the interrupted job left nothing queued or running: both slots serve new work once released
        a.close();
        b.close();
        Permit c = acquire(scheduler, 0, Duration.ofMillis(100));
        Permit d = acquire(scheduler, 0, Duration.ofMillis(100));
        c.close();
        d.close();
    }
}
