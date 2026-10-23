package com.snoozeshare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.infra.events.MessagePostedEvent;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;

/** Internal SYSTEM posting keeps the existing transaction and channel boundaries. */
@RecordApplicationEvents
class AssistantPostingTest extends AgentApiIntegrationTest {

    @Autowired
    private MessageService messages;
    @Autowired
    private PlatformTransactionManager transactions;
    @Autowired
    private ApplicationEvents events;

    @Test
    void seededSystemIdentityAndGuestAgentReadsE69() throws Exception {
        var dispute = scenarios.openDispute(RemedyType.FULL_REFUND);
        UUID ticketId = dispute.ticket().getId();
        Instant before = Instant.now();
        var saved = messages.postAssistant(ticketId, "  When did the noise start?  ");
        assertThat(saved.getAuthorId()).isEqualTo(AuditService.SYSTEM_ACTOR_ID);
        assertThat(saved.getAuthorRole()).isEqualTo(Role.SYSTEM);
        assertThat(saved.getChannel()).isEqualTo(ThreadChannel.GUEST);
        assertThat(saved.getBody()).isEqualTo("When did the noise start?");
        assertThat(saved.getSentAt()).isBetween(before, Instant.now());
        assertThat(jdbc.queryForObject("select id from users where role = 'SYSTEM'", UUID.class))
                .isEqualTo(AuditService.SYSTEM_ACTOR_ID);
        assertThat(jdbc.queryForObject("select firebase_uid from users where id = ?", String.class,
                AuditService.SYSTEM_ACTOR_ID)).isEqualTo("SYSTEM_INTERNAL");
        assertThat(messages.thread(ticketId, ThreadChannel.GUEST, dispute.guest().getId(), Role.GUEST))
                .extracting(com.snoozeshare.domain.model.Message::getId).containsExactly(saved.getId());
        mockMvc.perform(get("/api/agent/disputes/" + ticketId + "/messages/GUEST")
                .header("Authorization", "Bearer " + agent.token())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorRole").value("SYSTEM"))
                .andExpect(jsonPath("$[0].authorId").value(AuditService.SYSTEM_ACTOR_ID.toString()))
                .andExpect(jsonPath("$[0].body").value(saved.getBody()))
                .andExpect(jsonPath("$[0].sentAt").exists());
        mockMvc.perform(get("/api/guest/messages/tickets/" + ticketId)
                .header("Authorization", bearer(dispute.guest().getId(), "guest")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(saved.getId().toString()));
    }

    @Test
    void hostIsolationAndGuestUnreadIncreaseE25E61() throws Exception {
        var dispute = scenarios.openDispute(RemedyType.FULL_REFUND);
        UUID ticketId = dispute.ticket().getId();
        UUID guestId = dispute.guest().getId();
        UUID hostId = dispute.host().getId();
        assertThat(messages.unreadCount(guestId, Role.GUEST)).isZero();
        messages.postAssistant(ticketId, "When did it happen?");
        assertThat(messages.unreadCount(guestId, Role.GUEST)).isEqualTo(1);
        assertThat(messages.unreadCount(hostId, Role.HOST)).isZero();
        assertThat(messages.thread(ticketId, ThreadChannel.HOST, hostId, Role.HOST)).isEmpty();
        assertThat(messages.conversationsFor(hostId, Role.HOST)).singleElement()
                .satisfies(conversation -> {
                    assertThat(conversation.lastMessage()).isNull();
                    assertThat(conversation.unreadCount()).isZero();
                });
        assertThat(messages.conversationsFor(guestId, Role.GUEST)).singleElement()
                .satisfies(conversation -> {
                    assertThat(conversation.lastMessage().getAuthorRole()).isEqualTo(Role.SYSTEM);
                    assertThat(conversation.unreadCount()).isEqualTo(1);
                });
        mockMvc.perform(get("/api/host/messages/tickets/" + ticketId + "?channel=HOST")
                .header("Authorization", bearer(hostId, "host"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/host/messages/tickets")
                .header("Authorization", bearer(hostId, "host"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastMessage").value((Object) null))
                .andExpect(jsonPath("$[0].unreadCount").value(0));
        mockMvc.perform(get("/api/guest/messages/conversations")
                .header("Authorization", bearer(guestId, "guest"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketConversations[0].unreadCount").value(1));
    }

    @Test
    void internalPostingRejectsInvalidInputsWithoutRowsE75() {
        var dispute = scenarios.openDispute(RemedyType.FULL_REFUND);
        UUID id = dispute.ticket().getId();
        assertThatThrownBy(() -> messages.postAssistant(UUID.randomUUID(), "When?"))
                .isInstanceOf(NotFoundException.class);
        for (String body : new String[] {null, "", "  \n  ", "x".repeat(2001)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> messages.postAssistant(id, body));
        }
        assertThat(jdbc.queryForObject("select count(*) from messages", Long.class)).isZero();
        assertThat(messages.postAssistant(id, " " + "x".repeat(2000) + " ").getBody()).hasSize(2000);
        var resolved = scenarios.resolvedDispute(agent.id());
        assertThatIllegalStateException().isThrownBy(() -> messages.postAssistant(resolved.ticket().getId(), "When?"))
                .withMessage("Ticket is resolved");
    }

    @Test
    void publishesOnlyAfterCommitAndRollbackPublishesNothing() {
        var dispute = scenarios.openDispute(RemedyType.FULL_REFUND);
        UUID id = dispute.ticket().getId();
        events.clear();
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            messages.postAssistant(id, "When?");
            assertThat(events.stream(MessagePostedEvent.class)).isEmpty();
        });
        assertThat(events.stream(MessagePostedEvent.class)).hasSize(1);
        events.clear();
        tx.executeWithoutResult(status -> {
            messages.postAssistant(id, "Where?");
            status.setRollbackOnly();
        });
        assertThat(events.stream(MessagePostedEvent.class)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from messages where ticket_id = ?", Long.class, id))
                .isEqualTo(1);
    }

    private static String bearer(UUID id, String role) {
        return "Bearer agent-api-" + role + "-" + id;
    }
}
