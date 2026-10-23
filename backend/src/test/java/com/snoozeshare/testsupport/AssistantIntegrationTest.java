package com.snoozeshare.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.LlmGateway;
import com.snoozeshare.ai.LlmResult;
import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.model.Message;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.TicketAssistant;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.MessageRepository;
import com.snoozeshare.repository.TicketAssistantRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.MessageService;
import com.snoozeshare.service.assistant.AssistantApplier;
import com.snoozeshare.service.assistant.AssistantContextBuilder;
import com.snoozeshare.service.assistant.AssistantJob;
import com.snoozeshare.service.assistant.AssistantRunner;
import com.snoozeshare.service.assistant.ClarifyOutput;

/** One enabled context, shared fault-injection spies, and workers drained before every database reset. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {"snoozeshare.security.enabled=true", "snoozeshare.assistant.enabled=true"})
public abstract class AssistantIntegrationTest extends AgentApiIntegrationTest {
    @MockitoSpyBean protected LlmGateway gateway;
    @MockitoSpyBean protected TicketAssistantRepository assistants;
    @MockitoSpyBean protected TicketRepository tickets;
    @MockitoSpyBean protected BookingRepository bookings;
    @MockitoSpyBean protected UserRepository users;
    @MockitoSpyBean protected MessageRepository messages;
    @Autowired protected AssistantRunner runner;
    @Autowired protected AssistantJob job;
    @Autowired protected AssistantApplier applier;
    @Autowired protected AssistantContextBuilder context;
    @Autowired protected MessageService messageService;
    @Autowired protected Clock clock;
    @Autowired protected PlatformTransactionManager transactions;

    @Override
    @BeforeEach
    protected void resetApiFixture() {
        assertThat(runner.awaitIdle(Duration.ofSeconds(20))).isTrue();
        reset(gateway, assistants, tickets, bookings, users, messages, audit);
        doReturn(degraded(LlmErrorCode.DISABLED)).when(gateway).run(any(), any());
        super.resetApiFixture();
    }

    @Override
    @AfterEach
    protected void cleanApiFixture() {
        assertThat(runner.awaitIdle(Duration.ofSeconds(20))).isTrue();
        reset(assistants, tickets, bookings, users, messages, audit);
        super.cleanApiFixture();
    }

    /**
     * Files a ticket with the real listeners disabled by a DISABLED gateway, so the filing job removes its own row
     * and leaves no call or audit trace, then installs a clean PENDING row for the test to drive by hand.
     */
    protected Ticket pending() {
        doReturn(degraded(LlmErrorCode.DISABLED)).when(gateway).run(any(), any());
        Ticket ticket = scenarios.openDispute(RemedyType.FULL_REFUND).ticket();
        assertThat(runner.awaitIdle(Duration.ofSeconds(20))).isTrue();
        clearInvocations(gateway, audit);
        jdbc.update("delete from ticket_assistants where ticket_id = ?", ticket.getId());
        applier.insertPending(ticket.getId());
        return ticket;
    }

    protected TicketAssistant row(UUID id) {
        return assistants.findById(id).orElseThrow();
    }

    protected void rounds(UUID id, int rounds, String draft) {
        jdbc.update("update ticket_assistants set rounds = ?, draft = ? where ticket_id = ?", rounds, draft, id);
    }

    protected Message guestReply(Ticket ticket, String text) {
        return messages.saveAndFlush(new Message(UUID.randomUUID(), ticket.getId(), ThreadChannel.GUEST,
            ticket.getRaisedByUserId(), Role.GUEST, text, clock.instant()));
    }

    protected List<Message> thread(UUID id) {
        return messages.findThread(id, ThreadChannel.GUEST);
    }

    protected void tx(Runnable work) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> work.run());
    }

    protected static LlmResult<ClarifyOutput> ok(boolean enough, boolean flagged, String... questions) {
        return LlmResult.ok(new ClarifyOutput(enough, List.of(questions), "The Guest reports a problem.",
            List.of(), List.of(), List.of(), List.of(), List.of()), flagged, false, UUID.randomUUID());
    }

    protected static LlmResult<ClarifyOutput> degraded(LlmErrorCode code) {
        return LlmResult.degraded(ClarifyOutput.NONE, code, false, false, UUID.randomUUID());
    }
}
