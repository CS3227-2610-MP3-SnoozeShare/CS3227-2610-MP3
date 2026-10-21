package com.snoozeshare.infra.events;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.AccountStatus;

public record AccountStatusChangedEvent(
        UUID userId, AccountStatus newStatus,
        UUID actorId, Instant occurredAt) {
}
