package com.snoozeshare.api.controller.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.snoozeshare.domain.model.User;
import com.snoozeshare.service.AccountGovernanceService;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;

/** E74: two agents suspend one guest at the same time. */
class AgentAccountConcurrencyTest extends AgentApiIntegrationTest {

    @Autowired
    private AccountGovernanceService accounts;

    private final ExecutorService holder = Executors.newSingleThreadExecutor(r -> new Thread(r, "holder"));
    private final ExecutorService waiter = Executors.newSingleThreadExecutor(r -> new Thread(r, "waiter"));
    private final CountDownLatch entered = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final AtomicBoolean held = new AtomicBoolean();

    @AfterEach
    void stopThreads() {
        release.countDown();
        holder.shutdownNow();
        waiter.shutdownNow();
    }

    private Throwable outcome(Future<?> future) throws Exception {
        try {
            future.get(30, TimeUnit.SECONDS);
            return null;
        } catch (ExecutionException e) {
            return e.getCause();
        }
    }

    private long ledgerRows(UUID userId) {
        Long count = jdbc.queryForObject("select count(*) from audit_log where wallet_adjustment is not null "
                + "and subject_user_id = ?", Long.class, userId);
        return count == null ? 0 : count;
    }

    @Test
    void twoSuspendsRefundOnce() throws Exception {
        User target = scenarios.guestWithConfirmedFutureBooking();
        UUID targetId = target.getId();
        long before = ledgerRows(targetId);
        doAnswer(invocation -> {
            if ("holder".equals(Thread.currentThread().getName()) && held.compareAndSet(false, true)) {
                entered.countDown();
                release.await(20, TimeUnit.SECONDS);
            }
            return invocation.callRealMethod();
        }).when(audit).record(any());

        Future<?> a = holder.submit(() -> accounts.suspend(targetId, agent.id(), "first"));
        assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
        Future<?> b = waiter.submit(() -> accounts.suspend(targetId, agent.id(), "second"));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject("select count(*) from pg_stat_activity "
                    + "where datname = current_database() and wait_event_type = 'Lock'", Integer.class);
            if (waiting != null && waiting > 0) {
                break;
            }
            Thread.sleep(50);
        }
        release.countDown();

        assertThat(outcome(a)).isNull();
        assertThat(outcome(b)).isInstanceOf(IllegalStateException.class).hasMessage("Account is already suspended");
        assertThat(ledgerRows(targetId) - before).isEqualTo(1);
    }
}
