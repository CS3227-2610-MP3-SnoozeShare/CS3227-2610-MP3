package com.snoozeshare.ai;

import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.enums.Role;

/**
 * Everything a role feature hands the gateway for one call.
 *
 * @param userId the {@code users.id} of the caller, from the authorized principal (C79)
 * @param role the caller's role, from the same source
 * @param turns the conversation, oldest first; the last turn is a USER turn
 * @param trustedContext server-built text only, never user-authored; may be null
 * @param untrustedContext user-authored background text such as a ticket body; may be null
 * @param audit references copied to the audit row; null means {@link AuditContext#NONE}
 */
public record LlmCall(UUID userId, Role role, List<Turn> turns, String trustedContext, String untrustedContext,
                      AuditContext audit) {

    /** Normalizes a null audit context to {@link AuditContext#NONE}. */
    public LlmCall {
        if (audit == null) {
            audit = AuditContext.NONE;
        }
    }

    /**
     * Rejects a malformed call (pipeline step 1).
     *
     * @throws IllegalArgumentException when {@code userId}, {@code role} or {@code turns} is null, a turn or its
     *     role or text is null, {@code turns} is empty, or the last turn is not a USER turn
     */
    public void checkShape() {
        require(userId != null, "userId must not be null");
        require(role != null, "role must not be null");
        require(turns != null, "turns must not be null");
        require(!turns.isEmpty(), "turns must not be empty");
        for (Turn turn : turns) {
            require(turn != null, "turns must not contain null");
            require(turn.role() != null, "a turn must have a role");
            require(turn.text() != null, "a turn must have text");
        }
        require(turns.get(turns.size() - 1).role() == TurnRole.USER, "the last turn must be a USER turn");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException("LlmCall: " + message);
        }
    }
}
