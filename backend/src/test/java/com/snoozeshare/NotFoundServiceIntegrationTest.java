package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.service.AccountGovernanceService;
import com.snoozeshare.service.DisputeQueryService;
import com.snoozeshare.service.MessageService;
import com.snoozeshare.service.NotFoundException;
import com.snoozeshare.service.TicketService;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;

/** Ensures missing service resources have a typed failure for the API boundary. */
class NotFoundServiceIntegrationTest extends AgentApiIntegrationTest {

    @Autowired
    private DisputeQueryService disputes;

    @Autowired
    private MessageService messages;

    @Autowired
    private TicketService tickets;

    @Autowired
    private AccountGovernanceService accounts;

    @Test
    void missingDisputeDetailIsTypedNotFound() {
        assertThatThrownBy(() -> disputes.detail(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void missingMessageThreadIsTypedNotFound() {
        assertThatThrownBy(() -> messages.thread(UUID.randomUUID(), ThreadChannel.GUEST,
                guest.id(), Role.GUEST)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void missingTicketAssignmentIsTypedNotFound() {
        assertThatThrownBy(() -> tickets.assignToMe(UUID.randomUUID(), agent.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void missingGovernedUserIsTypedNotFound() {
        assertThatThrownBy(() -> accounts.suspend(UUID.randomUUID(), agent.id(), "Test reason"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void missingCategoryIsTypedNotFound() {
        assertThatThrownBy(() -> tickets.renameCategory(UUID.randomUUID(), "Renamed", agent.id()))
                .isInstanceOf(NotFoundException.class);
    }
}
