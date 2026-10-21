package com.snoozeshare.domain.statemachine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.Role;

class BookingStateMachineTest {

    @Test
    void nullInputsReturnFalse() {
        assertThat(BookingStateMachine.canTransition(null, BookingStatus.CONFIRMED, Role.HOST))
                .isFalse();
        assertThat(BookingStateMachine.canTransition(BookingStatus.PENDING, null, Role.HOST))
                .isFalse();
        assertThat(BookingStateMachine.canTransition(BookingStatus.PENDING, BookingStatus.CONFIRMED, null))
                .isFalse();
    }

    // --- PENDING transitions ---

    @Test
    void hostCanConfirmPending() {
        assertThat(BookingStateMachine.canTransition(
                BookingStatus.PENDING, BookingStatus.CONFIRMED, Role.HOST)).isTrue();
    }

    @Test
    void hostCanRejectPending() {
        assertThat(BookingStateMachine.canTransition(
                BookingStatus.PENDING, BookingStatus.REJECTED, Role.HOST)).isTrue();
    }

    @Test
    void guestCanCancelPending() {
        assertThat(BookingStateMachine.canTransition(
                BookingStatus.PENDING, BookingStatus.CANCELLED_BY_GUEST, Role.GUEST)).isTrue();
    }

    @Test
    void agentCanForceCancelPending() {
        assertThat(BookingStateMachine.canTransition(
                BookingStatus.PENDING, BookingStatus.FORCE_CANCELLED, Role.AGENT)).isTrue();
    }

    @Test
    void guestCannotConfirmPending() {
        assertThat(BookingStateMachine.canTransition(
                BookingStatus.PENDING, BookingStatus.CONFIRMED, Role.GUEST)).isFalse();
    }

    // --- CONFIRMED transitions ---

    @Test
    void guestCanCancelConfirmed() {
        assertThat(BookingStateMachine.canTransition(
                BookingStatus.CONFIRMED, BookingStatus.CANCELLED_BY_GUEST, Role.GUEST)).isTrue();
    }

    @Test
    void hostCanCancelConfirmed() {
        assertThat(BookingStateMachine.canTransition(
                BookingStatus.CONFIRMED, BookingStatus.CANCELLED_BY_HOST, Role.HOST)).isTrue();
    }

    @Test
    void hostCanCompleteConfirmed() {
        assertThat(BookingStateMachine.canTransition(
                BookingStatus.CONFIRMED, BookingStatus.COMPLETED, Role.HOST)).isTrue();
    }

    @Test
    void agentCanCompleteConfirmed() {
        assertThat(BookingStateMachine.canTransition(
                BookingStatus.CONFIRMED, BookingStatus.COMPLETED, Role.AGENT)).isTrue();
    }

    @Test
    void agentCanForceCancelConfirmed() {
        assertThat(BookingStateMachine.canTransition(
                BookingStatus.CONFIRMED, BookingStatus.FORCE_CANCELLED, Role.AGENT)).isTrue();
    }

    @Test
    void agentCanForceCompleteConfirmed() {
        assertThat(BookingStateMachine.canTransition(
                BookingStatus.CONFIRMED, BookingStatus.FORCE_COMPLETED, Role.AGENT)).isTrue();
    }

    // --- Terminal states ---

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {
        "REJECTED", "CANCELLED_BY_GUEST", "CANCELLED_BY_HOST",
        "COMPLETED", "FORCE_CANCELLED", "FORCE_COMPLETED"
    })
    void terminalStatesCannotTransition(BookingStatus terminal) {
        for (BookingStatus target : BookingStatus.values()) {
            for (Role role : Role.values()) {
                assertThat(BookingStateMachine.canTransition(terminal, target, role))
                        .as("from %s to %s as %s", terminal, target, role)
                        .isFalse();
            }
        }
    }
}
