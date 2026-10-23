package com.snoozeshare.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.model.Message;
import com.snoozeshare.domain.model.TicketAssistant.State;
import com.snoozeshare.infra.events.MessagePostedEvent;
import com.snoozeshare.infra.events.TicketOpenedEvent;
import com.snoozeshare.testsupport.AssistantIntegrationTest;
import com.snoozeshare.testsupport.DisputeScenarioFactory.Scenario;

/** Real filing and posting commits drive the listeners; no test method is transactional. */
class AssistantTriggerTest extends AssistantIntegrationTest {
    private static final Duration WAIT = Duration.ofSeconds(20);
    @MockitoSpyBean private AssistantRunner runnerSpy;
    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private com.snoozeshare.service.TicketService ticketService;

    private Scenario file() {
        Scenario scenario = scenarios.openDispute(RemedyType.FULL_REFUND);
        idle();
        return scenario;
    }

    private void idle() {
        assertThat(runner.awaitIdle(WAIT)).isTrue();
    }

    private Message guestPost(Scenario s, String text) {
        return messageService.post(s.ticket().getId(), ThreadChannel.GUEST, s.guest().getId(), Role.GUEST, text);
    }

    private Message agentPost(Scenario s, ThreadChannel channel) {
        return messageService.post(s.ticket().getId(), channel, s.agent().getId(), Role.AGENT, "Agent here");
    }

    private void asking() {
        doAnswer(invocation -> ok(false, false, "When did it start?")).when(gateway).run(any(), any());
    }

    @Test
    void filingStartsOneCallAndTheAssistantsOwnMessageDoesNotLoopE20() {
        asking();
        Scenario s = file();
        UUID id = s.ticket().getId();
        verify(gateway, times(1)).run(any(), any());
        assertThat(row(id).getState()).isEqualTo(State.WAITING_GUEST);
        assertThat(row(id).getRounds()).isEqualTo((short) 1);
        assertThat(thread(id).stream().filter(m -> m.getAuthorRole() == Role.SYSTEM)).hasSize(1);
    }

    @Test
    void guestReplyResumesOnceAndDuplicateDeliveryAddsNoCallE26() {
        asking();
        Scenario s = file();
        UUID id = s.ticket().getId();
        Message reply = guestPost(s, "It started on Monday");
        publisher.publishEvent(new MessagePostedEvent(reply.getId(), id, reply.getSentAt()));
        idle();
        verify(gateway, times(2)).run(any(), any());
        assertThat(row(id).getRounds()).isEqualTo((short) 2);
    }

    @Test
    void agentPostInGuestChannelStopsButHostChannelAndAssignmentDoNotE16E19E60() {
        asking();
        Scenario s = file();
        UUID id = s.ticket().getId();
        agentPost(s, ThreadChannel.HOST);
        ticketService.assignToMe(id, s.agent().getId());
        assertThat(row(id).getState()).isEqualTo(State.WAITING_GUEST);
        agentPost(s, ThreadChannel.GUEST);
        assertThat(row(id).getState()).isEqualTo(State.STOPPED);
        guestPost(s, "ignored");
        idle();
        verify(gateway, times(1)).run(any(), any());
    }

    @Test
    void duplicateSubmissionEventLeavesTheRowAndCallCountE64() {
        asking();
        Scenario s = file();
        UUID id = s.ticket().getId();
        tx(() -> publisher.publishEvent(new TicketOpenedEvent(id, s.guest().getId(), clock.instant())));
        idle();
        verify(gateway, times(1)).run(any(), any());
        assertThat(row(id).getRounds()).isEqualTo((short) 1);
    }

    @Test
    void failuresInsideListenersNeverReachFilingOrPostingE65E66() {
        asking();
        doThrow(new IllegalStateException("insert down")).when(assistants).insertPending(any(), any());
        Scenario failed = scenarios.openDispute(RemedyType.FULL_REFUND);
        idle();
        assertThat(assistants.findById(failed.ticket().getId())).isEmpty();
        reset(assistants);

        Scenario s = file();
        UUID id = s.ticket().getId();
        doThrow(new IllegalStateException("claim down")).when(assistants).claimWaiting(any(), any());
        guestPost(s, "reply while the claim fails");
        idle();
        assertThat(row(id).getState()).isEqualTo(State.WAITING_GUEST);
        reset(assistants);
        doThrow(new IllegalStateException("lock down")).when(assistants).findByTicketIdForUpdate(any());
        agentPost(s, ThreadChannel.GUEST);
        reset(assistants);
        assertThat(row(id).getState()).isEqualTo(State.WAITING_GUEST);
    }

    @Test
    void rejectedJobsEndUnavailableForFilingAndReplyE31() {
        doThrow(new RejectedExecutionException("full")).when(runnerSpy).submit(any());
        Scenario s = scenarios.openDispute(RemedyType.FULL_REFUND);
        assertThat(row(s.ticket().getId()).getState()).isEqualTo(State.UNAVAILABLE);

        reset(runnerSpy);
        asking();
        Scenario t = file();
        doThrow(new RejectedExecutionException("full")).when(runnerSpy).submit(any());
        guestPost(t, "reply");
        assertThat(row(t.ticket().getId()).getState()).isEqualTo(State.UNAVAILABLE);
        assertThat(thread(t.ticket().getId()).stream().filter(m -> ClarifyRendering.isClosing(m.getBody())))
            .hasSize(1);
        reset(runnerSpy);
    }

    @Test
    void slowGatewayNeverBlocksFilingOrPostingE32() {
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            release.await(30, TimeUnit.SECONDS);
            return ok(false, false, "When did it start?");
        }).when(gateway).run(any(), any());
        try {
            Scenario s = scenarios.openDispute(RemedyType.FULL_REFUND);
            assertThat(row(s.ticket().getId()).getState()).isEqualTo(State.PENDING);
            long started = System.nanoTime();
            guestPost(s, "a note while the call is running");
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
        } finally {
            release.countDown();
            idle();
        }
    }

    @Test
    void midCallRepliesThroughRealEventsCoalesceAndTheCapEndsTheChainE8E15() {
        AtomicInteger calls = new AtomicInteger();
        Scenario[] box = new Scenario[1];
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 2) {
                for (int i = 0; i < 3; i++) {
                    guestPost(box[0], "extra " + i);
                }
            }
            return ok(false, false, "When did it start?");
        }).when(gateway).run(any(), any());
        box[0] = scenarios.openDispute(RemedyType.FULL_REFUND);
        UUID id = box[0].ticket().getId();
        idle();
        guestPost(box[0], "first answer");
        idle();
        guestPost(box[0], "answer to round three");
        idle();
        assertThat(row(id).getState()).isEqualTo(State.DONE);
        assertThat(row(id).getRounds()).isEqualTo((short) 3);
        assertThat(calls.get()).isEqualTo(4);
    }

    @Test
    void agentPostDuringACallStopsAndTheLateResultIsDiscardedE17E58() {
        Scenario[] box = new Scenario[1];
        doAnswer(invocation -> {
            agentPost(box[0], ThreadChannel.GUEST);
            return ok(false, false, "When did it start?");
        }).when(gateway).run(any(), any());
        box[0] = scenarios.openDispute(RemedyType.FULL_REFUND);
        idle();
        UUID id = box[0].ticket().getId();
        assertThat(row(id).getState()).isEqualTo(State.STOPPED);
        assertThat(thread(id).stream().filter(m -> m.getAuthorRole() == Role.SYSTEM)).isEmpty();
    }
}
