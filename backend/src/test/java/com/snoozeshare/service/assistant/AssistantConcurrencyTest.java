package com.snoozeshare.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.model.Message;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.TicketAssistant.State;
import com.snoozeshare.repository.RowLocks;
import com.snoozeshare.testsupport.AssistantIntegrationTest;

class AssistantConcurrencyTest extends AssistantIntegrationTest {
    @Autowired private RowLocks locks;

    @Test
    void capEndpointsAndConfigurationReductionRefreshExactlyOnceE15E57E81() {
        for (int cap : List.of(1, 3, 5)) {
            var ticket = pending();
            var configured = new AssistantProperties(true, cap, 1, 10, 10);
            var configuredApplier = new AssistantApplier(tickets, assistants, messages, messageService,
                audit, locks, configured, clock, transactions);
            int calls = 0;
            while (true) {
                calls++;
                boolean waiting = configuredApplier.apply(ticket.getId(), null, ok(false, false, "When?"));
                if (!waiting) {
                    break;
                }
                guestReply(ticket, "answer " + calls);
                assertThat(configuredApplier.claimWaiting(ticket.getId())).isTrue();
            }
            assertThat(calls).isEqualTo(cap + 1);
            assertThat(row(ticket.getId()).getRounds()).isEqualTo((short) cap);
            assertThat(row(ticket.getId()).getState()).isEqualTo(State.DONE);
            assertThat(thread(ticket.getId()).stream().filter(m -> ClarifyRendering.isQuestion(m.getBody())))
                .hasSize(cap);
            assertThat(thread(ticket.getId()).stream().filter(m -> ClarifyRendering.isClosing(m.getBody())))
                .hasSize(1);
        }
        var lowered = pending();
        rounds(lowered.getId(), 3, "old draft");
        var configuredApplier = new AssistantApplier(tickets, assistants, messages, messageService, audit, locks,
            new AssistantProperties(true, 1, 1, 10, 10), clock, transactions);
        configuredApplier.apply(lowered.getId(), null, ok(false, false, "When?"));
        assertThat(row(lowered.getId()).getState()).isEqualTo(State.DONE);
        assertThat(row(lowered.getId()).getRounds()).isEqualTo((short) 3);
        assertThat(row(lowered.getId()).getDraft()).isNotEqualTo("old draft");
        assertThat(thread(lowered.getId())).singleElement().extracting(Message::getBody)
            .isEqualTo(ClarifyRendering.CLOSING);
    }

    @Test
    void threeMidCallRepliesCoalesceIntoOneFollowupE8() {
        var ticket = pending();
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 2) {
                for (int i = 0; i < 3; i++) {
                    guestReply(ticket, "extra " + i);
                }
            }
            return ok(false, false, "When?");
        }).when(gateway).run(any(), any());
        job.submit(ticket.getId());
        assertThat(runner.awaitIdle(Duration.ofSeconds(20))).isTrue();
        guestReply(ticket, "first answer");
        assertThat(applier.claimWaiting(ticket.getId())).isTrue();
        job.submit(ticket.getId());
        assertThat(runner.awaitIdle(Duration.ofSeconds(20))).isTrue();
        assertThat(calls).hasValue(3);
        assertThat(row(ticket.getId()).getRounds()).isEqualTo((short) 3);
        assertThat(row(ticket.getId()).getState()).isEqualTo(State.WAITING_GUEST);
    }

    @Test
    void fiftyRepliesRemainBoundedByFourCallsE49() {
        var ticket = pending();
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            int call = calls.incrementAndGet();
            if (call == 2 || call == 3) {
                int burst = call == 2 ? 24 : 25;
                for (int i = 0; i < burst; i++) {
                    guestReply(ticket, "burst " + call + "/" + i);
                }
            }
            return ok(false, false, "When?");
        }).when(gateway).run(any(), any());
        job.submit(ticket.getId());
        assertThat(runner.awaitIdle(Duration.ofSeconds(30))).isTrue();
        guestReply(ticket, "start burst");
        assertThat(applier.claimWaiting(ticket.getId())).isTrue();
        job.submit(ticket.getId());
        assertThat(runner.awaitIdle(Duration.ofSeconds(30))).isTrue();
        assertThat(calls).hasValue(4);
        assertThat(row(ticket.getId()).getRounds()).isEqualTo((short) 3);
        assertThat(row(ticket.getId()).getState()).isEqualTo(State.DONE);
        assertThat(thread(ticket.getId()).stream().filter(m -> m.getAuthorRole() == Role.SYSTEM)).hasSize(4);
    }

    @Test
    void singleBurstDuringSecondCallNeedsThreeCallsUntilALaterReplyE94() {
        var ticket = pending();
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 2) {
                for (int i = 0; i < 49; i++) {
                    guestReply(ticket, "burst " + i);
                }
            }
            return ok(false, false, "When?");
        }).when(gateway).run(any(), any());
        job.submit(ticket.getId());
        assertThat(runner.awaitIdle(Duration.ofSeconds(30))).isTrue();
        guestReply(ticket, "start burst");
        assertThat(applier.claimWaiting(ticket.getId())).isTrue();
        job.submit(ticket.getId());
        assertThat(runner.awaitIdle(Duration.ofSeconds(30))).isTrue();
        assertThat(calls).hasValue(3);
        assertThat(row(ticket.getId()).getRounds()).isEqualTo((short) 3);
        assertThat(row(ticket.getId()).getState()).isEqualTo(State.WAITING_GUEST);
        assertThat(thread(ticket.getId()).stream().filter(m -> ClarifyRendering.isClosing(m.getBody()))).isEmpty();
        guestReply(ticket, "later reply");
        assertThat(applier.claimWaiting(ticket.getId())).isTrue();
        job.submit(ticket.getId());
        assertThat(runner.awaitIdle(Duration.ofSeconds(30))).isTrue();
        assertThat(calls).hasValue(4);
        assertThat(row(ticket.getId()).getState()).isEqualTo(State.DONE);
        assertThat(thread(ticket.getId()).stream().filter(m -> ClarifyRendering.isClosing(m.getBody()))).hasSize(1);
    }

    @Test
    void agentTakeoverBeforeOrDuringFirstReplyAndFailedListenerAllStopE17E58E85E86() {
        for (int prior : List.of(0, 1)) {
            for (boolean stopListener : List.of(false, true)) {
                var ticket = pending();
                rounds(ticket.getId(), prior, "prior draft");
                doAnswer(invocation -> {
                    agentPost(ticket);
                    if (stopListener) {
                        applier.stop(ticket.getId());
                    }
                    return ok(false, false, "When?");
                }).when(gateway).run(any(), any());
                job.run(ticket.getId());
                assertThat(row(ticket.getId()).getState()).isEqualTo(State.STOPPED);
                assertThat(row(ticket.getId()).getDraft()).isEqualTo("prior draft");
                assertThat(thread(ticket.getId()).stream().filter(m -> m.getAuthorRole() == Role.SYSTEM)).isEmpty();
            }
        }
        var beforeRow = pending();
        jdbc.update("delete from ticket_assistants where ticket_id = ?", beforeRow.getId());
        agentPost(beforeRow);
        applier.insertPending(beforeRow.getId());
        job.run(beforeRow.getId());
        assertThat(row(beforeRow.getId()).getState()).isEqualTo(State.STOPPED);
    }

    @Test
    void resolvedDuringCallDiscardsResultE18() {
        var ticket = pending();
        doAnswer(invocation -> {
            jdbc.update("update tickets set status = 'RESOLVED_REJECTED' where id = ?", ticket.getId());
            return ok(false, false, "When?");
        }).when(gateway).run(any(), any());
        job.run(ticket.getId());
        assertThat(row(ticket.getId()).getState()).isEqualTo(State.PENDING);
        assertThat(row(ticket.getId()).getDraft()).isNull();
        assertThat(thread(ticket.getId())).isEmpty();
    }

    @Test
    void concurrentAppliesCommitOnlyOneRoundAndClaimRaceOnlyOneWinnerE27E88() throws Exception {
        var ticket = pending();
        var pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            var one = pool.submit(() -> {
                start.await();
                return applier.apply(ticket.getId(), null, ok(false, false, "When?"));
            });
            var two = pool.submit(() -> {
                start.await();
                return applier.apply(ticket.getId(), null, ok(false, false, "Where?"));
            });
            start.countDown();
            assertThat(List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
            assertThat(row(ticket.getId()).getRounds()).isEqualTo((short) 1);
            assertThat(thread(ticket.getId())).hasSize(1);
            var claimOne = pool.submit(() -> applier.claimWaiting(ticket.getId()));
            var claimTwo = pool.submit(() -> applier.claimWaiting(ticket.getId()));
            assertThat(List.of(claimOne.get(15, TimeUnit.SECONDS), claimTwo.get(15, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void agentStopWinsInBothCommitOrdersE63() {
        var stopFirst = pending();
        applier.stop(stopFirst.getId());
        applier.apply(stopFirst.getId(), null, ok(false, false, "When?"));
        assertThat(row(stopFirst.getId()).getState()).isEqualTo(State.STOPPED);
        assertThat(thread(stopFirst.getId())).isEmpty();
        var applyFirst = pending();
        applier.apply(applyFirst.getId(), null, ok(false, false, "When?"));
        applier.stop(applyFirst.getId());
        assertThat(row(applyFirst.getId()).getState()).isEqualTo(State.STOPPED);
        assertThat(thread(applyFirst.getId())).hasSize(1);
    }

    @Test
    void lockedAgentMutationTimesOutWithoutChangingPendingRowE28() throws Exception {
        var ticket = pending();
        var pool = Executors.newSingleThreadExecutor();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var holding = pool.submit(() -> tx(() -> {
            tickets.findByIdForUpdate(ticket.getId()).orElseThrow();
            locked.countDown();
            try {
                assertThat(release.await(15, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(failure);
            }
        }));
        try {
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> applier.apply(ticket.getId(), null, ok(false, false, "When?")))
                .isInstanceOf(RuntimeException.class);
        } finally {
            release.countDown();
            holding.get(10, TimeUnit.SECONDS);
            pool.shutdownNow();
        }
        assertThat(row(ticket.getId()).getState()).isEqualTo(State.PENDING);
        assertThat(thread(ticket.getId())).isEmpty();
    }

    private void agentPost(Ticket ticket) {
        messages.saveAndFlush(new Message(UUID.randomUUID(), ticket.getId(), ThreadChannel.GUEST,
            agent.id(), Role.AGENT, "Agent takes over", clock.instant()));
    }
}
