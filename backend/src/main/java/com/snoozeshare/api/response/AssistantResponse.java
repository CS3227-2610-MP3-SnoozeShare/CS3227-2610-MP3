package com.snoozeshare.api.response;

import java.time.Instant;

/** Private Agent read contract; no persistence or gateway bookkeeping. */
public record AssistantResponse(String state, int rounds, int maxRounds, boolean flagged,
                                boolean stale, String draft, Instant draftUpdatedAt) {
}
