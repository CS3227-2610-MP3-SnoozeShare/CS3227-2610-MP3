package com.snoozeshare.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.enums.Role;

/** E4: a malformed call is a caller bug and throws before anything else happens. */
class LlmCallTest {

    private static final List<Turn> ONE_USER_TURN = List.of(new Turn(TurnRole.USER, "hello"));

    private static LlmCall call(UUID userId, Role role, List<Turn> turns) {
        return new LlmCall(userId, role, turns, null, null, null);
    }

    @Test
    void validCallPasses() {
        LlmCall call = call(UUID.randomUUID(), Role.GUEST, ONE_USER_TURN);
        assertThatCode(call::checkShape).doesNotThrowAnyException();
        assertThat(call.audit()).isSameAs(AuditContext.NONE);
    }

    @Test
    void shapeErrors() {
        List<Turn> withNullElement = new ArrayList<>();
        withNullElement.add(null);
        withNullElement.add(new Turn(TurnRole.USER, "x"));
        List<LlmCall> bad = List.of(
            call(null, Role.GUEST, ONE_USER_TURN),
            call(UUID.randomUUID(), null, ONE_USER_TURN),
            call(UUID.randomUUID(), Role.GUEST, null),
            call(UUID.randomUUID(), Role.GUEST, List.of()),
            call(UUID.randomUUID(), Role.GUEST, withNullElement),
            call(UUID.randomUUID(), Role.GUEST, List.of(new Turn(TurnRole.USER, null))),
            call(UUID.randomUUID(), Role.GUEST, List.of(new Turn(null, "x"))),
            call(UUID.randomUUID(), Role.GUEST,
                List.of(new Turn(TurnRole.USER, "q"), new Turn(TurnRole.ASSISTANT, "a"))));
        for (LlmCall call : bad) {
            assertThatThrownBy(call::checkShape).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
