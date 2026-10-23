package com.snoozeshare.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.repository.TicketAssistantRepository;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;

/** E11: with the switch off, no assistant bean exists and filing leaves no assistant row. */
class AssistantSwitchTest extends AgentApiIntegrationTest {
    @Autowired private ApplicationContext context;
    @Autowired private TicketAssistantRepository assistants;

    @Test
    void disabledSwitchRegistersNoRuntimeBeansAndFilingCreatesNoRowE11() {
        assertThat(context.getBeansOfType(TicketOpenedListener.class)).isEmpty();
        assertThat(context.getBeansOfType(MessagePostedListener.class)).isEmpty();
        assertThat(context.getBeansOfType(AssistantRunner.class)).isEmpty();
        var ticket = scenarios.openDispute(RemedyType.FULL_REFUND).ticket();
        assertThat(assistants.findById(ticket.getId())).isEmpty();
    }
}
