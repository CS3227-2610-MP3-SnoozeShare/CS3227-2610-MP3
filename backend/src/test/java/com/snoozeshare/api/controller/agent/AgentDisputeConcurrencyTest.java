package com.snoozeshare.api.controller.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.ResolutionMode;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.service.AuditService;
import com.snoozeshare.service.TicketService;
import com.snoozeshare.service.requests.ResolutionRequest;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;
import com.snoozeshare.testsupport.DisputeScenarioFactory.Scenario;

/** E10, E20 and E76: two real transactions race for one ticket. */
class AgentDisputeConcurrencyTest extends AgentApiIntegrationTest {

    @Autowired
    private TicketService tickets;

    @MockitoSpyBean
    private AuditService audit;

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

    private void parkHolderAtFirstAuditWrite() {
        doAnswer(invocation -> {
            if ("holder".equals(Thread.currentThread().getName()) && held.compareAndSet(false, true)) {
                entered.countDown();
                release.await(20, TimeUnit.SECONDS);
            }
            return invocation.callRealMethod();
        }).when(audit).record(any());
    }

    private void awaitCompetingLockWait() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject(
                    "select count(*) from pg_stat_activity where datname = current_database() "
                            + "and wait_event_type = 'Lock'", Integer.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(50);
        }
    }

    private void awaitHolderParked(Future<?> holderTask) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (entered.await(50, TimeUnit.MILLISECONDS)) {
                return;
            }
            if (holderTask.isDone()) {
                throw new AssertionError("holder ended before parking", outcome(holderTask));
            }
        }
        throw new AssertionError("holder never reached its first audit write");
    }

    private Throwable outcome(Future<?> future) throws Exception {
        try {
            future.get(30, TimeUnit.SECONDS);
            return null;
        } catch (java.util.concurrent.ExecutionException e) {
            return e.getCause();
        }
    }

    private BigDecimal balanceOf(UUID userId) {
        return jdbc.queryForObject("select balance from wallets where user_id = ?", BigDecimal.class, userId);
    }

    private long auditRows(String action) {
        Long count = jdbc.queryForObject("select count(*) from audit_log where action_type = ?", Long.class, action);
        return count == null ? 0 : count;
    }

    @Test
    void twoAssignsOneWins() throws Exception { // E10
        Scenario scenario = scenarios.openDispute(RemedyType.FULL_REFUND);
        UUID ticketId = scenario.ticket().getId();
        User first = scenario.agent();
        User second = testUsers.create(com.snoozeshare.domain.enums.Role.AGENT).user();
        long before = auditRows("TICKET_ASSIGNED");
        parkHolderAtFirstAuditWrite();

        Future<?> a = holder.submit(() -> tickets.assignToMe(ticketId, first.getId()));
        awaitHolderParked(a);
        Future<?> b = waiter.submit(() -> tickets.assignToMe(ticketId, second.getId()));
        awaitCompetingLockWait();
        release.countDown();

        Throwable resultA = outcome(a);
        Throwable resultB = outcome(b);
        assertThat(resultA).isNull();
        assertThat(resultB).isInstanceOf(IllegalStateException.class).hasMessage("Ticket is not open");
        assertThat(auditRows("TICKET_ASSIGNED") - before).isEqualTo(1);
    }

    @Test
    void twoResolvesSettleOnce() throws Exception { // E20
        Scenario scenario = scenarios.inReviewDispute(agent.id());
        UUID ticketId = scenario.ticket().getId();
        UUID guestId = scenario.guest().getId();
        BigDecimal guestBefore = balanceOf(guestId);
        long before = auditRows("TICKET_RESOLVED");
        ResolutionRequest request = new ResolutionRequest(ResolutionMode.MANUAL, new BigDecimal("1.00"), "race");
        parkHolderAtFirstAuditWrite();

        Future<?> a = holder.submit(() -> tickets.resolve(ticketId, request, agent.id()));
        awaitHolderParked(a);
        Future<?> b = waiter.submit(() -> tickets.resolve(ticketId, request, agent.id()));
        awaitCompetingLockWait();
        release.countDown();

        assertThat(outcome(a)).isNull();
        assertThat(outcome(b)).isInstanceOf(IllegalStateException.class).hasMessage("Ticket is not in review");
        assertThat(balanceOf(guestId).subtract(guestBefore)).isEqualByComparingTo("1.00");
        assertThat(auditRows("TICKET_RESOLVED") - before).isEqualTo(1);
    }

    @Test
    void lockTimeoutIs409() throws Exception { // E76 (takes more than 5 s by design)
        Scenario scenario = scenarios.inReviewDispute(agent.id());
        UUID ticketId = scenario.ticket().getId();
        ResolutionRequest request = new ResolutionRequest(ResolutionMode.REJECT, null, "slow");
        parkHolderAtFirstAuditWrite();

        Future<?> a = holder.submit(() -> tickets.resolve(ticketId, request, agent.id()));
        awaitHolderParked(a);
        Future<?> b = waiter.submit(() -> tickets.resolve(ticketId, request, agent.id()));
        Throwable waited = outcome(b);
        release.countDown();

        assertThat(waited).isInstanceOf(PessimisticLockingFailureException.class);
        assertThat(outcome(a)).isNull();
    }
}
