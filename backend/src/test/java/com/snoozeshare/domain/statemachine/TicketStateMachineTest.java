package com.snoozeshare.domain.statemachine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.TicketStatus;

class TicketStateMachineTest {

    @Test
    void nullInputsReturnFalse() {
        assertThat(TicketStateMachine.canTransition(null, TicketStatus.IN_REVIEW, Role.AGENT))
                .isFalse();
        assertThat(TicketStateMachine.canTransition(TicketStatus.OPEN, null, Role.AGENT))
                .isFalse();
    }

    @Test
    void onlyAgentCanTransition() {
        assertThat(TicketStateMachine.canTransition(
                TicketStatus.OPEN, TicketStatus.IN_REVIEW, Role.GUEST)).isFalse();
        assertThat(TicketStateMachine.canTransition(
                TicketStatus.OPEN, TicketStatus.IN_REVIEW, Role.HOST)).isFalse();
    }

    @Test
    void agentCanMoveOpenToInReview() {
        assertThat(TicketStateMachine.canTransition(
                TicketStatus.OPEN, TicketStatus.IN_REVIEW, Role.AGENT)).isTrue();
    }

    @Test
    void agentCanResolveInReview() {
        assertThat(TicketStateMachine.canTransition(
                TicketStatus.IN_REVIEW, TicketStatus.RESOLVED_APPROVED, Role.AGENT)).isTrue();
        assertThat(TicketStateMachine.canTransition(
                TicketStatus.IN_REVIEW, TicketStatus.RESOLVED_REJECTED, Role.AGENT)).isTrue();
    }

    @Test
    void agentCanReopenInReview() {
        assertThat(TicketStateMachine.canTransition(
                TicketStatus.IN_REVIEW, TicketStatus.OPEN, Role.AGENT)).isTrue();
    }

    @Test
    void resolvedStatesAreTerminal() {
        for (TicketStatus target : TicketStatus.values()) {
            assertThat(TicketStateMachine.canTransition(
                    TicketStatus.RESOLVED_APPROVED, target, Role.AGENT))
                    .as("RESOLVED_APPROVED to %s", target).isFalse();
            assertThat(TicketStateMachine.canTransition(
                    TicketStatus.RESOLVED_REJECTED, target, Role.AGENT))
                    .as("RESOLVED_REJECTED to %s", target).isFalse();
        }
    }
}
