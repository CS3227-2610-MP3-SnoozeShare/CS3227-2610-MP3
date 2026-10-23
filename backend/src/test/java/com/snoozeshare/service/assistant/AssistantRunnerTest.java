package com.snoozeshare.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Real workers only: queue capacity, follow-up accounting and shutdown (R10). */
class AssistantRunnerTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void boundedDaemonWorkerNeverUsesRequestThreadAndRejectsOverflowE31E32() throws Exception {
        var runner = new AssistantRunner(new AssistantProperties(true, 3, 1, 1, 10));
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var worker = new AtomicReference<Thread>();
        var transaction = new AtomicBoolean(true);
        Thread submitting = Thread.currentThread();
        try {
            TransactionSynchronizationManager.setActualTransactionActive(true);
            runner.submit(() -> {
                worker.set(Thread.currentThread());
                transaction.set(TransactionSynchronizationManager.isActualTransactionActive());
                entered.countDown();
                await(release);
            });
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            runner.submit(() -> { });
            assertThatThrownBy(() -> runner.submit(() -> { })).isInstanceOf(RejectedExecutionException.class);
            assertThat(runner.awaitIdle(Duration.ZERO)).isFalse();
            assertThat(worker.get()).isNotEqualTo(submitting);
            assertThat(worker.get().isDaemon()).isTrue();
            assertThat(worker.get().getName()).matches("assistant-\\d+");
            assertThat(transaction).isFalse();
            release.countDown();
            assertThat(runner.awaitIdle(TIMEOUT)).isTrue();
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            release.countDown();
            runner.close();
        }
    }

    @Test
    void idleIncludesFollowUpHandoffAndInjectedExecutorIsAsynchronous() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        var runner = new AssistantRunner(pool);
        var childEntered = new CountDownLatch(1);
        var childRelease = new CountDownLatch(1);
        try {
            runner.submit(() -> runner.submit(() -> {
                childEntered.countDown();
                await(childRelease);
            }));
            assertThat(childEntered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(runner.awaitIdle(Duration.ofMillis(20))).isFalse();
            childRelease.countDown();
            assertThat(runner.awaitIdle(TIMEOUT)).isTrue();
        } finally {
            childRelease.countDown();
            runner.close();
            pool.shutdownNow();
        }
    }

    @Test
    void rejectionDoesNotLeavePhantomOutstandingWork() {
        var runner = new AssistantRunner(command -> {
            throw new RejectedExecutionException("Synthetic rejection");
        });
        try {
            assertThatThrownBy(() -> runner.submit(() -> { })).isInstanceOf(RejectedExecutionException.class);
            assertThat(runner.awaitIdle(Duration.ZERO)).isTrue();
        } finally {
            runner.close();
        }
    }

    @Test
    void shutdownInterruptsWorkerCancelsQueuedWorkAndRejectsNewWork() throws Exception {
        var runner = new AssistantRunner(new AssistantProperties(true, 3, 1, 1, 10));
        var entered = new CountDownLatch(1);
        var interrupted = new AtomicBoolean();
        var queuedRan = new AtomicBoolean();
        runner.submit(() -> {
            entered.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException expected) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
        });
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        runner.submit(() -> queuedRan.set(true));
        runner.close();
        assertThat(runner.awaitIdle(TIMEOUT)).isTrue();
        assertThat(interrupted).isTrue();
        assertThat(queuedRan).isFalse();
        assertThatThrownBy(() -> runner.submit(() -> { })).isInstanceOf(RejectedExecutionException.class);
        runner.close();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Synthetic worker did not release");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
